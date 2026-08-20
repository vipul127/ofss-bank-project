/**
 * @license
 * Copyright (c) 2014, 2026, Oracle and/or its affiliates.
 * Licensed under The Universal Permissive License (UPL), Version 1.0
 * as shown at https://oss.oracle.com/licenses/upl/
 * @ignore
 */
/**
 * A top-level require call executed by the Application.
 * Although 'knockout' would be loaded in any case (it is specified as a  dependency
 * by some modules), we are listing it explicitly to get the reference to the 'ko'
 * object in the callback
 */
require(['ojs/ojbootstrap', 'ojs/ojcontext', 'knockout', 'ojs/ojknockout', './services/apiClient'],
  function (Bootstrap, Context, ko, ojKnockout, apiClient) {
    Bootstrap.whenDocumentReady().then(
      function () {
        function init() {
          var accounts = {
            branch001: { password: 'branch001', role: 'BR001', branchName: 'Andheri West' },
            branch002: { password: 'branch002', role: 'BR002', branchName: 'Bandra Kurla Complex' },
            branch003: { password: 'branch003', role: 'BR003', branchName: 'Powai' },
            branch004: { password: 'branch004', role: 'BR004', branchName: 'Thane' }
          };
          var loginView = document.getElementById('login-view');
          var consoleView = document.getElementById('console-view');
          var loginForm = document.getElementById('login-form');
          var loginError = document.getElementById('login-error');
          var branchSummary = document.getElementById('branch-summary');
          var logoutButton = document.getElementById('logout-button');
          var dashboardError = document.getElementById('dashboard-error');

          function money(value) {
            var numeric = Number(value);
            return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number.isFinite(numeric) ? numeric : 0);
          }

          function normalizeAnalysis(analysis) {
            analysis = analysis || {};
            var points = analysis.dailyPoints || [];
            var last = points.length ? points[points.length - 1] : {};
            var nets = points.map(function (point) { return Number(point.netMovement); }).filter(Number.isFinite);
            var average = Number(analysis.averageDailyNet);
            if (!Number.isFinite(average)) average = nets.length ? nets.reduce(function (sum, value) { return sum + value; }, 0) / nets.length : 0;
            var todayNet = Number(analysis.todayNet);
            if (!Number.isFinite(todayNet)) todayNet = Number(last.netMovement) || 0;
            analysis.averageDailyNet = average;
            analysis.todayNet = todayNet;
            analysis.todayDeposits = Number.isFinite(Number(analysis.todayDeposits)) ? Number(analysis.todayDeposits) : Math.max(todayNet, 0);
            analysis.todayWithdrawals = Number.isFinite(Number(analysis.todayWithdrawals)) ? Number(analysis.todayWithdrawals) : Math.max(-todayNet, 0);
            analysis.reserveBeforeToday = Number.isFinite(Number(analysis.reserveBeforeToday)) ? Number(analysis.reserveBeforeToday) : Number(last.closingReserve || 0) - todayNet;
            analysis.todayVsAverageAmount = Number.isFinite(Number(analysis.todayVsAverageAmount)) ? Number(analysis.todayVsAverageAmount) : todayNet - average;
            analysis.todayVsAveragePercent = Number.isFinite(Number(analysis.todayVsAveragePercent)) ? Number(analysis.todayVsAveragePercent) : (average ? Math.abs(todayNet - average) * 100 / Math.abs(average) : 0);
            ['standardDeviation', 'averageSurplus', 'averageDeficit', 'withdrawalImpactPercent'].forEach(function (key) {
              if (!Number.isFinite(Number(analysis[key]))) analysis[key] = 0;
            });
            return analysis;
          }

          function renderChart(analysis, forecast) {
            var chart = document.getElementById('cash-chart');
            var points = analysis.dailyPoints || [];
            if (!points.length) { chart.innerHTML = '<p class="muted-text">No daily history yet.</p>'; return; }
            var reserves = points.map(function (point) { return Number(point.closingReserve); });
            var min = Math.min.apply(null, reserves.concat([Number(forecast.confidenceBandLow)]));
            var max = Math.max.apply(null, reserves.concat([Number(forecast.confidenceBandHigh)]));
            var range = max - min || 1;
            var width = 720, height = 150;
            function x(i) { return 12 + i * (width - 24) / Math.max(points.length - 1, 1); }
            function y(v) { return height - 12 - (v - min) * (height - 24) / range; }
            var actual = reserves.map(function (v, i) { return x(i) + ',' + y(v); }).join(' ');
            var lastX = x(reserves.length - 1), lastY = y(reserves[reserves.length - 1]);
            var forecastY = y(Number(forecast.predictedPosition));
            var bandTop = y(Number(forecast.confidenceBandHigh));
            var bandBottom = y(Number(forecast.confidenceBandLow));
            chart.innerHTML = '<svg viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="Actual transaction line and forecast deviation band">' +
              '<polygon class="chart-band" points="' + lastX + ',' + bandTop + ' ' + width + ',' + bandTop + ' ' + width + ',' + bandBottom + ' ' + lastX + ',' + bandBottom + '"></polygon>' +
              '<polyline class="chart-line actual" points="' + actual + '"></polyline>' +
              '<line class="chart-line forecast" x1="' + lastX + '" y1="' + lastY + '" x2="' + (width - 12) + '" y2="' + forecastY + '"></line>' +
              '<circle class="chart-point" cx="' + lastX + '" cy="' + lastY + '" r="4"></circle></svg>';
            document.getElementById('chart-milestones').innerHTML = points.map(function (point) {
              return '<span title="' + point.date + ': ' + money(point.closingReserve) + '">' + point.date.slice(5) + '</span>';
            }).join('');
            var direction = reserves[reserves.length - 1] >= reserves[0] ? 'rising' : 'declining';
            var directionText = direction === 'rising' ? 'Rising' : 'Declining';
            var signal = document.getElementById('trajectory-signal');
            signal.textContent = directionText;
            signal.className = 'signal-badge ' + direction;
            document.getElementById('trajectory-title').textContent = directionText + ' over 14 days';
            document.getElementById('forecast-read').textContent = direction === 'rising' ? 'Cash position is building.' : 'Cash position is drawing down.';
          }

          function renderForecastDetail(analysis, forecast, position) {
            var direction = Number(analysis.averageDailyNet) >= 0 ? 'positive' : 'negative';
            var detail = document.getElementById('forecast-detail');
            var todayDirection = Number(analysis.todayNet) >= 0 ? 'positive' : 'negative';
            detail.innerHTML = '<article class="forecast-detail-card"><span class="detail-label">Reserve before today</span><strong>' + money(analysis.reserveBeforeToday) + '</strong><small>Opening buffer before today’s activity</small></article>' +
              '<article class="forecast-detail-card positive"><span class="detail-label">Today deposits</span><strong>+' + money(analysis.todayDeposits) + '</strong><small>Cash added today</small></article>' +
              '<article class="forecast-detail-card negative"><span class="detail-label">Today withdrawals</span><strong>-' + money(analysis.todayWithdrawals) + '</strong><small>Cash removed today</small></article>' +
              '<article class="forecast-detail-card ' + todayDirection + '"><span class="detail-label">Today net</span><strong>' + money(analysis.todayNet) + '</strong><small>' + (Number(analysis.todayNet) >= 0 ? 'Improved' : 'Reduced') + ' the reserve</small></article>' +
              '<article class="forecast-detail-card ' + (Number(analysis.todayVsAverageAmount) >= 0 ? 'positive' : 'negative') + '"><span class="detail-label">Today vs 14-day average</span><strong>' + money(analysis.todayVsAverageAmount) + '</strong><small>' + Number(analysis.todayVsAveragePercent).toFixed(1) + '% ' + (Number(analysis.todayVsAverageAmount) >= 0 ? 'above' : 'below') + ' average net movement</small></article>' +
              '<article class="forecast-detail-card ' + direction + '"><span class="detail-label">Average daily net</span><strong>' + money(analysis.averageDailyNet) + '</strong><small>Fourteen-day bundled movement</small></article>' +
              '<article class="forecast-detail-card"><span class="detail-label">Volatility</span><strong>' + money(analysis.standardDeviation) + '</strong><small>Standard deviation of daily movement</small></article>' +
              '<article class="forecast-detail-card ' + (Number(analysis.averageSurplus) > 0 ? 'positive' : '') + '"><span class="detail-label">Average surplus</span><strong>' + money(analysis.averageSurplus) + '</strong><small>Typical positive cash build per active day</small></article>' +
              '<article class="forecast-detail-card ' + (Number(analysis.averageDeficit) > 0 ? 'negative' : '') + '"><span class="detail-label">Average funding need</span><strong>' + money(analysis.averageDeficit) + '</strong><small>Typical negative cash requirement per active day</small></article>' +
              '<article class="forecast-detail-card"><span class="detail-label">Withdrawal shock</span><strong>' + Number(analysis.withdrawalImpactPercent).toFixed(1) + '%</strong><small>Largest recorded withdrawal against current reserve</small></article>' +
              '<article class="forecast-detail-card"><span class="detail-label">Tomorrow confidence</span><strong>' + money(forecast.confidenceBandLow) + ' – ' + money(forecast.confidenceBandHigh) + '</strong><small>Expected position: ' + money(forecast.predictedPosition) + '</small></article>';
            var impactClass = Number(analysis.todayNet) >= 0 ? 'positive' : 'negative';
            document.getElementById('forecast-hero').innerHTML = '<article class="forecast-impact ' + impactClass + '"><span class="impact-label">Today reserve impact</span><strong>' + (Number(analysis.todayVsAveragePercent) >= 0 ? '+' : '-') + Number(analysis.todayVsAveragePercent).toFixed(1) + '%</strong><small>Today’s net movement versus the 14-day average</small></article>' +
              '<article class="forecast-hero-card"><span class="hero-label">Today net movement</span><strong>' + money(analysis.todayNet) + '</strong><small>Deposits ' + money(analysis.todayDeposits) + ' · Withdrawals ' + money(analysis.todayWithdrawals) + '</small></article>' +
              '<article class="forecast-hero-card"><span class="hero-label">Tomorrow estimate</span><strong>' + money(forecast.predictedPosition) + '</strong><small>Range ' + money(forecast.confidenceBandLow) + ' to ' + money(forecast.confidenceBandHigh) + '</small></article>';
            document.getElementById('forecast-net-banner').className = 'forecast-net-banner ' + (Number(analysis.todayNet) < 0 ? 'negative' : '');
            document.getElementById('forecast-net-banner').innerHTML = '<span class="impact-label">Net surplus added to / removed from reserve today</span><strong>' + (Number(analysis.todayNet) >= 0 ? '+' : '') + money(analysis.todayNet) + '</strong>';
          }

          function renderNearbyBranches(branches) {
            document.getElementById('nearby-branches').innerHTML = branches.length ? branches.map(function (branch) {
              var status = (branch.status || 'UNKNOWN').toLowerCase();
              var amount = Number(branch.surplusOrDeficitAmount) || 0;
              var action = status === 'surplus' ? '<div class="nearby-request"><input type="number" min="1" step="1000" placeholder="Amount" aria-label="Amount to request from ' + branch.branchName + '" data-amount-for="' + branch.branchId + '"><button type="button" data-request-from="' + branch.branchId + '">Raise request</button></div>' : '';
              return '<article class="nearby-card"><h3>' + branch.branchName + '</h3><small>' + Number(branch.distanceKm).toFixed(1) + ' km away · ' + branch.branchId + '</small><strong>' + money(Math.abs(amount)) + '</strong><small>' + (amount >= 0 ? 'available surplus' : 'estimated cash need') + '</small><span class="nearby-status ' + status + '">' + (branch.status || 'UNKNOWN') + '</span>' + action + '</article>';
            }).join('') : '<p class="muted-text">No nearby branch data available.</p>';
          }

          function renderTransferList(transfers, role) {
            var own = transfers.filter(function (transfer) { return transfer.destinationBranchId === role || transfer.sourceBranchId === role; });
            document.getElementById('transfers-list').innerHTML = own.length ? own.map(function (transfer) {
              var receiving = transfer.destinationBranchId === role;
              var action = transfer.status === 'REQUESTED' ? (receiving ? '<button class="transfer-action" data-approve="' + transfer.requestId + '">Approve</button>' : '') + '<button class="transfer-action revoke" data-revoke="' + transfer.requestId + '">Revoke</button>' : '';
              return '<div class="list-row"><span>' + (receiving ? 'Request from ' + transfer.sourceBranchId : 'Request to ' + transfer.destinationBranchId) + '<small>' + transfer.requestId + ' · ' + transfer.status + '</small></span><span><strong>' + money(transfer.amount) + '</strong>' + action + '</span></div>';
            }).join('') : '<p class="muted-text">No transfer requests for this branch.</p>';
          }

          function showToast(message, type) {
            var toast = document.getElementById('toast');
            toast.textContent = message; toast.className = 'toast ' + type; toast.hidden = false;
            window.clearTimeout(window.toastTimer); window.toastTimer = window.setTimeout(function () { toast.hidden = true; }, 3600);
          }

          function activeSession() {
            return JSON.parse(sessionStorage.getItem('branchCashSession'));
          }

          function checkHealth(id, url, headers) {
            fetch(url, { method: 'GET', headers: headers || {}, cache: 'no-store' }).then(function (response) {
              document.getElementById(id).className = response.ok ? 'online' : 'offline';
            }).catch(function () { document.getElementById(id).className = 'offline'; });
          }

          function refreshHealth() {
            var branchId = (activeSession() || {}).role || 'BR004';
            var headers = { 'X-Branch-Role': branchId };
            checkHealth('health-eureka', 'http://localhost:8761/eureka/apps');
            checkHealth('health-branch', 'http://localhost:8081/api/branches/' + branchId, headers);
            checkHealth('health-cash', 'http://localhost:8082/api/cash-position/' + branchId, headers);
            checkHealth('health-forecast', 'http://localhost:8083/api/forecast/' + branchId, headers);
            checkHealth('health-requirement', 'http://localhost:8084/api/transfer-requests', headers);
            fetch('http://localhost:8761/eureka/apps', { cache: 'no-store' }).then(function (response) {
              return response.text();
            }).then(function (text) {
              document.getElementById('health-simulator').className = text.indexOf('SIMULATOR-SERVICE') >= 0 || text.indexOf('simulator-service') >= 0 ? 'online' : 'offline';
            }).catch(function () { document.getElementById('health-simulator').className = 'offline'; });
          }

          function loadDashboard(account) {
            var branchId = account.role;
            dashboardError.hidden = true;
            Promise.all([apiClient.getPosition(branchId), apiClient.getForecast(branchId), apiClient.getRecentTransactions(branchId), apiClient.getTransferRequests(), apiClient.getDailyAnalysis(branchId), apiClient.getNearbyBranches(branchId)])
              .then(function (results) {
                var position = results[0];
                var forecast = results[1];
                var transactions = results[2];
                var transfers = results[3];
                var analysis = results[4];
                var nearby = results[5];
                analysis = normalizeAnalysis(analysis);
                renderNearbyBranches(nearby);
                renderChart(analysis, forecast);
                renderForecastDetail(analysis, forecast, position);
                document.getElementById('forecast-chart').innerHTML = document.getElementById('cash-chart').innerHTML;
                document.getElementById('forecast-chart-milestones').innerHTML = document.getElementById('chart-milestones').innerHTML;
                document.getElementById('reserve-value').textContent = money(position.currentReserve);
                document.getElementById('status-value').textContent = position.status;
                var positionPercent = Number(position.currentReserve) === 0 ? 0 : Number(position.surplusOrDeficitAmount) / Number(position.currentReserve) * 100;
                document.getElementById('net-position-value').textContent = money(position.surplusOrDeficitAmount) + ' · ' + Math.abs(positionPercent).toFixed(1) + '% ' + position.status.toLowerCase();
                document.getElementById('status-value').className = position.status === 'SURPLUS' ? 'positive-value' : 'negative-value';
                document.getElementById('stddev-value').textContent = money(analysis.standardDeviation);
                document.getElementById('surplus-value').textContent = money(analysis.averageSurplus);
                document.getElementById('deficit-value').textContent = money(analysis.averageDeficit);
                document.getElementById('impact-value').textContent = Number(analysis.withdrawalImpactPercent).toFixed(1) + '%';
                document.getElementById('forecast-value').textContent = money(forecast.predictedPosition);
                document.getElementById('forecast-band').textContent = 'Range ' + money(forecast.confidenceBandLow) + ' to ' + money(forecast.confidenceBandHigh);
                document.getElementById('forecast-detail').innerHTML = '<strong>' + money(forecast.predictedPosition) + '</strong><p class="muted-text">Confidence band: ' + money(forecast.confidenceBandLow) + ' - ' + money(forecast.confidenceBandHigh) + '</p>';
                document.getElementById('transactions-list').innerHTML = transactions.length ? transactions.map(function (transaction) {
                  return '<div class="list-row"><span>' + transaction.txnType + '<small>' + new Date(transaction.eventTimestamp).toLocaleString() + '</small></span><strong>' + money(transaction.amount) + '</strong></div>';
                }).join('') : '<p class="muted-text">No transactions recorded yet.</p>';
                renderTransferList(transfers, branchId);
                document.getElementById('threshold-input').value = position.thresholdAmount / position.currentReserve * 100;
              }).catch(function (error) {
                dashboardError.textContent = 'Live data could not be loaded. Check that the backend services are running.';
                dashboardError.hidden = false;
                console.error(error);
              });
          }

          function showConsole(account, username) {
            loginView.hidden = true;
            consoleView.hidden = false;
            branchSummary.textContent = username + ' is assigned to ' + account.role + ' - ' + account.branchName + '.';
            document.getElementById('sidebar-role').textContent = account.role;
            loadDashboard(account);
            refreshHealth();
            window.clearInterval(window.branchHealthTimer);
            window.branchHealthTimer = window.setInterval(refreshHealth, 15000);
          }

          var storedSession = sessionStorage.getItem('branchCashSession');
          if (storedSession) {
            var session = JSON.parse(storedSession);
            if (accounts[session.username] && accounts[session.username].role === session.role) {
              showConsole(accounts[session.username], session.username);
            } else {
              sessionStorage.removeItem('branchCashSession');
            }
          }

          loginForm.addEventListener('submit', function (event) {
            event.preventDefault();
            var username = loginForm.username.value.trim().toLowerCase();
            var password = loginForm.password.value;
            var account = accounts[username];
            if (!account || account.password !== password) {
              loginError.textContent = 'Enter a valid prototype username and password.';
              loginError.hidden = false;
              return;
            }
            loginError.hidden = true;
            sessionStorage.setItem('branchCashSession', JSON.stringify({ username: username, role: account.role }));
            showConsole(account, username);
          });

          logoutButton.addEventListener('click', function () {
            sessionStorage.removeItem('branchCashSession');
            consoleView.hidden = true;
            loginView.hidden = false;
            loginForm.reset();
            window.clearInterval(window.branchHealthTimer);
            document.querySelectorAll('[data-panel]').forEach(function (panel) { panel.hidden = panel.dataset.panel !== 'overview'; });
            document.querySelectorAll('.tab-button').forEach(function (item) { item.classList.toggle('active', item.dataset.section === 'overview'); });
          });

          document.querySelectorAll('.tab-button').forEach(function (button) {
            button.addEventListener('click', function () {
              document.querySelectorAll('.tab-button').forEach(function (item) { item.classList.remove('active'); });
              document.querySelectorAll('[data-panel]').forEach(function (panel) { panel.hidden = panel.dataset.panel !== button.dataset.section; });
              button.classList.add('active');
            });
          });

          document.getElementById('threshold-form').addEventListener('submit', function (event) {
            event.preventDefault();
            var session = activeSession();
            apiClient.updateThreshold(session.role, Number(document.getElementById('threshold-input').value))
              .then(function () { document.getElementById('threshold-message').textContent = 'Threshold saved for ' + session.role + '.'; loadDashboard(accounts[session.username]); })
              .catch(function () { document.getElementById('threshold-message').textContent = 'Threshold could not be saved.'; });
          });

          document.getElementById('nearby-branches').addEventListener('click', function (event) {
            var button = event.target.closest('[data-request-from]');
            if (!button) return;
            var source = button.getAttribute('data-request-from');
            var amountInput = document.querySelector('[data-amount-for="' + source + '"]');
            var amount = Number(amountInput.value);
            var session = activeSession();
            if (!amount || amount <= 0) { showToast('Enter a positive amount to request.', 'error'); amountInput.focus(); return; }
            button.disabled = true;
            apiClient.createTransferRequest(session.role, source, amount).then(function () {
              showToast('Request raised to ' + source + ' for approval.', 'success');
              amountInput.value = '';
              return apiClient.getTransferRequests();
            }).then(function (transfers) {
              var ownTransfers = transfers.filter(function (transfer) { return transfer.destinationBranchId === session.role; });
              document.getElementById('transfers-list').innerHTML = ownTransfers.length ? ownTransfers.map(function (transfer) {
                return '<div class="list-row"><span>From ' + transfer.sourceBranchId + '<small>' + transfer.requestId + '</small></span><strong>' + transfer.status + '</strong></div>';
              }).join('') : '<p class="muted-text">No transfer requests for this branch.</p>';
            }).catch(function () { showToast('Transfer request could not be raised.', 'error'); }).finally(function () { button.disabled = false; });
          });

          document.getElementById('transfers-list').addEventListener('click', function (event) {
            var approve = event.target.closest('[data-approve]');
            var revoke = event.target.closest('[data-revoke]');
            var session = activeSession();
            if (approve) apiClient.updateTransferStatus(approve.getAttribute('data-approve'), 'APPROVED').then(function () { showToast('Transfer approved.', 'success'); loadDashboard(accounts[session.username]); }).catch(function () { showToast('Transfer could not be approved.', 'error'); });
            if (revoke) apiClient.revokeTransferRequest(revoke.getAttribute('data-revoke')).then(function () { showToast('Transfer request revoked.', 'success'); loadDashboard(accounts[session.username]); }).catch(function () { showToast('Transfer could not be revoked.', 'error'); });
          });
        }
        // If running in a hybrid (e.g. Cordova) environment, we need to wait for the deviceready
        // event before executing any code that might interact with Cordova APIs or plugins.
        if (document.body.classList.contains('oj-hybrid')) {
          document.addEventListener('deviceready', init);
        } else {
          init();
        }
        // release the application bootstrap busy state
        Context.getPageContext().getBusyContext().applicationBootstrapComplete();
      });
  }
);
