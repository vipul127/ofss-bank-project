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
            ,bankadmin: { password: 'bankadmin', role: 'BANK_ADMIN', branchName: 'All branches' }
          };
          var loginView = document.getElementById('login-view');
          var consoleView = document.getElementById('console-view');
          var loginForm = document.getElementById('login-form');
          var loginError = document.getElementById('login-error');
          var branchSummary = document.getElementById('branch-summary');
          var logoutButton = document.getElementById('logout-button');
          var dashboardError = document.getElementById('dashboard-error');
          var adminView = document.getElementById('admin-view');
          var adminLogout = document.getElementById('admin-logout');

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
            var predVal = Number(forecast.predictedPosition) || reserves[reserves.length - 1] || 0;
            var min = Math.min.apply(null, reserves.concat([predVal]));
            var max = Math.max.apply(null, reserves.concat([predVal]));
            var range = max - min || 1;
            var width = 720, height = 150;

            function x(i) { return 24 + i * (width - 48) / Math.max(points.length - 1, 1); }
            function y(v) { return height - 20 - (v - min) * (height - 40) / range; }

            var actualPoints = reserves.map(function (v, i) { return x(i) + ',' + y(v); }).join(' ');
            var lastX = x(reserves.length - 1);
            var lastY = y(reserves[reserves.length - 1]);
            var forecastY = y(predVal);

            // Compute standard deviation threshold for identifying spikes
            var stdDev = Number(analysis.standardDeviation) || 30000;

            // Generate SVG elements for every single day dot
            var dotsHtml = points.map(function (point, i) {
              var cx = x(i);
              var cy = y(Number(point.closingReserve));
              var net = Number(point.netMovement) || 0;
              var isNegativeSpike = net < 0 && Math.abs(net) >= (stdDev * 0.75 || 30000);
              var isPositiveSpike = net > 0 && net >= (stdDev * 0.75 || 30000);
              
              var dotClass = 'chart-dot';
              if (isNegativeSpike) dotClass += ' spike-negative';
              else if (isPositiveSpike) dotClass += ' spike-positive';

              var titleStr = (point.date ? point.date : '') + ' | Reserve: ' + money(point.closingReserve) + ' | Net: ' + (net >= 0 ? '+' : '') + money(net);
              return '<circle class="' + dotClass + '" cx="' + cx + '" cy="' + cy + '" r="' + (isNegativeSpike ? '6' : '4.5') + '" ' +
                'data-date="' + (point.date || '') + '" ' +
                'data-reserve="' + money(point.closingReserve) + '" ' +
                'data-net="' + ((net >= 0 ? '+' : '') + money(net)) + '" ' +
                'data-spike="' + (isNegativeSpike ? 'WITHDRAWAL_SPIKE' : (isPositiveSpike ? 'DEPOSIT_SURGE' : 'NORMAL')) + '">' +
                '<title>' + titleStr + '</title></circle>';
            }).join('');

            chart.style.position = 'relative';
            chart.innerHTML = '<svg viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="14-day cash health trajectory graph">' +
              '<polyline class="chart-line actual" points="' + actualPoints + '"></polyline>' +
              '<line class="chart-line forecast" x1="' + lastX + '" y1="' + lastY + '" x2="' + (width - 16) + '" y2="' + forecastY + '"></line>' +
              '<circle class="chart-point" cx="' + (width - 16) + '" cy="' + forecastY + '" r="5" fill="var(--accent)"></circle>' +
              dotsHtml + '</svg>' +
              '<div id="chart-tooltip" class="chart-tooltip-box" hidden></div>';

            // Attach interactive snap hover handlers for chart dots
            var tooltip = document.getElementById('chart-tooltip');
            chart.querySelectorAll('.chart-dot').forEach(function (dot) {
              dot.addEventListener('mouseenter', function (e) {
                var dt = dot.getAttribute('data-date');
                var res = dot.getAttribute('data-reserve');
                var net = dot.getAttribute('data-net');
                var spike = dot.getAttribute('data-spike');
                
                var tagHtml = '';
                if (spike === 'WITHDRAWAL_SPIKE') tagHtml = '<span class="tag-spike">⚠️ Heavy Withdrawal Spike</span>';
                else if (spike === 'DEPOSIT_SURGE') tagHtml = '<span class="tag-deposit">🟢 Strong Deposit Inflow</span>';

                tooltip.innerHTML = '<strong>' + res + '</strong>' +
                  '<small>Date: ' + dt + ' · Daily Net: ' + net + '</small>' + tagHtml;
                
                var cx = Number(dot.getAttribute('cx'));
                var cy = Number(dot.getAttribute('cy'));
                
                if (cy < 45) {
                  tooltip.classList.add('bottom-flip');
                } else {
                  tooltip.classList.remove('bottom-flip');
                }
                
                tooltip.style.left = (cx / width * 100) + '%';
                tooltip.style.top = (cy / height * 100) + '%';
                tooltip.hidden = false;
              });

              dot.addEventListener('mouseleave', function () {
                tooltip.hidden = true;
              });
            });

            document.getElementById('chart-milestones').innerHTML = points.map(function (point, i) {
              return '<span title="' + point.date + ': ' + money(point.closingReserve) + '">' + (point.date ? point.date.slice(5) : '') + '</span>';
            }).join('');

            var direction = reserves[reserves.length - 1] >= reserves[0] ? 'rising' : 'declining';
            var directionText = direction === 'rising' ? 'Rising' : 'Declining';
            var signal = document.getElementById('trajectory-signal');
            if (signal) {
              signal.textContent = directionText;
              signal.className = 'signal-badge ' + direction;
            }
            document.getElementById('trajectory-title').textContent = directionText + ' over 14 days';
            document.getElementById('forecast-read').textContent = direction === 'rising' ? 'Cash position is building.' : 'Cash position is drawing down under withdrawal pressure.';
          }

          function renderForecastDetail(analysis, forecast, position) {
            var direction = Number(analysis.averageDailyNet) >= 0 ? 'positive' : 'negative';
            var detail = document.getElementById('forecast-detail');
            var todayDirection = Number(analysis.todayNet) > 0 ? 'positive' : (Number(analysis.todayNet) < 0 ? 'negative' : 'neutral');
            detail.innerHTML = '<article class="forecast-detail-card"><span class="detail-label">Reserve before today</span><strong>' + money(analysis.reserveBeforeToday) + '</strong><small>Opening buffer before today’s activity</small></article>' +
              '<article class="forecast-detail-card positive"><span class="detail-label">Today deposits</span><strong>+' + money(analysis.todayDeposits) + '</strong><small>Cash added today</small></article>' +
              '<article class="forecast-detail-card negative"><span class="detail-label">Today withdrawals</span><strong>-' + money(analysis.todayWithdrawals) + '</strong><small>Cash removed today</small></article>' +
              '<article class="forecast-detail-card ' + todayDirection + '"><span class="detail-label">Today net</span><strong>' + money(analysis.todayNet) + '</strong><small>' + (Number(analysis.todayNet) > 0 ? 'Improved' : (Number(analysis.todayNet) < 0 ? 'Reduced' : 'No cash movement recorded')) + '</small></article>' +
              '<article class="forecast-detail-card ' + (Number(analysis.todayVsAverageAmount) >= 0 ? 'positive' : 'negative') + '"><span class="detail-label">Today vs 14-day average</span><strong>' + money(analysis.todayVsAverageAmount) + '</strong><small>' + Number(analysis.todayVsAveragePercent).toFixed(1) + '% ' + (Number(analysis.todayVsAverageAmount) >= 0 ? 'above' : 'below') + ' average net movement</small></article>' +
              '<article class="forecast-detail-card ' + direction + '"><span class="detail-label">Average daily net</span><strong>' + money(analysis.averageDailyNet) + '</strong><small>Average across 14 calendar days</small></article>' +
              '<article class="forecast-detail-card"><span class="detail-label">Volatility (Severity)</span><strong>' + money(analysis.standardDeviation) + '</strong><small>Standard deviation of daily net movement</small></article>' +
              '<article class="forecast-detail-card ' + (Number(analysis.averageSurplus) > 0 ? 'positive' : '') + '"><span class="detail-label">Average surplus</span><strong>' + money(analysis.averageSurplus) + '</strong><small>Typical positive cash build per active day</small></article>' +
              '<article class="forecast-detail-card ' + (Number(analysis.averageDeficit) > 0 ? 'negative' : '') + '"><span class="detail-label">Average funding need</span><strong>' + money(analysis.averageDeficit) + '</strong><small>Typical negative cash requirement per active day</small></article>' +
              '<article class="forecast-detail-card"><span class="detail-label">Withdrawal shock</span><strong>' + Number(analysis.withdrawalImpactPercent).toFixed(1) + '%</strong><small>Largest recorded withdrawal against current reserve</small></article>';
            var impactClass = Number(analysis.todayNet) >= 0 ? 'positive' : 'negative';
            var signSymbol = Number(analysis.todayNet) >= 0 ? '+' : '-';
            document.getElementById('forecast-hero').innerHTML = '<article class="forecast-impact ' + impactClass + '"><span class="impact-label">Today reserve impact</span><strong>' + signSymbol + Math.abs(Number(analysis.todayVsAveragePercent)).toFixed(1) + '%</strong><small>Today’s net movement versus the 14-day average</small></article>' +
              '<article class="forecast-hero-card"><span class="hero-label">Today net movement</span><strong>' + money(analysis.todayNet) + '</strong><small>Deposits ' + money(analysis.todayDeposits) + ' · Withdrawals ' + money(analysis.todayWithdrawals) + '</small></article>' +
              '<article class="forecast-hero-card"><span class="hero-label">Projected EOD Reserve</span><strong>' + money(forecast.predictedPosition) + '</strong><small>Assumes tomorrow demand matches recent operational flow</small></article>';
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
            Promise.all([apiClient.getPosition(branchId), apiClient.getForecast(branchId), apiClient.getRecentTransactions(branchId), apiClient.getTransferRequests(), apiClient.getDailyAnalysis(branchId), apiClient.getNearbyBranches(branchId), apiClient.getAiExplanation(branchId).catch(function () { return null;
              
            })])
              .then(function (results) {
                var position = results[0];
                var forecast = results[1];
                var transactions = results[2];
                var transfers = results[3];
                var analysis = results[4];
                var nearby = results[5];
                var ai = results[6];
                analysis = normalizeAnalysis(analysis);
                renderNearbyBranches(nearby);
                renderChart(analysis, forecast);
                renderForecastDetail(analysis, forecast, position);
                document.getElementById('forecast-chart').innerHTML = document.getElementById('cash-chart').innerHTML;
                document.getElementById('forecast-chart-milestones').innerHTML = document.getElementById('chart-milestones').innerHTML;
                document.getElementById('reserve-value').textContent = money(position.currentReserve);
                var baseline = Number(position.thresholdAmount) || Number(position.currentReserve) || 1;
                var positionPercent = (Number(position.surplusOrDeficitAmount) / baseline) * 100;
                var formattedAmount = (Number(position.surplusOrDeficitAmount) >= 0 ? '+' : '') + money(position.surplusOrDeficitAmount);
                document.getElementById('net-position-value').textContent = formattedAmount + ' · ' + Math.abs(positionPercent).toFixed(1) + '% ' + position.status.toLowerCase();
                document.getElementById('status-value').textContent = position.status;
                document.getElementById('status-value').className = position.status === 'SURPLUS' ? 'positive-value' : 'negative-value';
                document.getElementById('stddev-value').textContent = money(analysis.standardDeviation);
                document.getElementById('surplus-value').textContent = money(analysis.averageSurplus);
                document.getElementById('deficit-value').textContent = money(analysis.averageDeficit);
                document.getElementById('impact-value').textContent = Number(analysis.withdrawalImpactPercent).toFixed(1) + '%';
                document.getElementById('forecast-value').textContent = money(forecast.predictedPosition);
                document.getElementById('forecast-band').textContent = 'Assumes recent operational flow';
                document.getElementById('transactions-list').innerHTML = transactions.length ? transactions.map(function (transaction) {
                  return '<div class="list-row"><span>' + transaction.txnType + '<small>' + new Date(transaction.eventTimestamp).toLocaleString() + '</small></span><strong>' + money(transaction.amount) + '</strong></div>';
                }).join('') : '<p class="muted-text">No transactions recorded yet.</p>';
                renderTransferList(transfers, branchId);
                document.getElementById('threshold-input').value = position.thresholdAmount / position.currentReserve * 100;
                if (ai) {
                  document.getElementById('ai-headline').textContent = ai.headline || 'Branch liquidity diagnosis';
                  document.getElementById('ai-reason').textContent = ai.reason || 'AI explanation unavailable.';
                  document.getElementById('ai-action').textContent = ai.recommendedAction || 'Continue monitoring daily cash movement.';
                  document.getElementById('ai-source').textContent = ai.source || 'Cash analysis';
                }
              }).catch(function (error) {
                dashboardError.textContent = 'Live data could not be loaded: ' + (error && error.message ? error.message : 'one dashboard request failed') + '.';
                dashboardError.hidden = false;
                console.error(error);
              });
          }

          var currentAdminBranchId = null;
          var cachedAdminBranches = [];

          function inspectBranch(branchId) {
            currentAdminBranchId = branchId;
            var inspector = document.getElementById('admin-inspector');
            if (!inspector) return;
            inspector.hidden = false;

            // Highlight selected row in branch register
            document.querySelectorAll('.admin-row').forEach(function (row) {
              row.classList.toggle('active', row.getAttribute('data-branch-id') === branchId);
            });

            // Update branch switcher buttons
            var switcher = document.getElementById('inspector-switcher');
            switcher.innerHTML = cachedAdminBranches.map(function (b) {
              var isActive = b.branchId === branchId;
              return '<button type="button" class="switcher-btn ' + (isActive ? 'active' : '') + '" data-switch-branch="' + b.branchId + '">' + b.branchId + ' - ' + b.branchName + '</button>';
            }).join('');

            var selectedBranch = cachedAdminBranches.find(function (b) { return b.branchId === branchId; }) || {};
            document.getElementById('inspector-branch-name').textContent = (selectedBranch.branchName || branchId) + ' (' + branchId + ')';
            document.getElementById('inspector-branch-meta').textContent = 'Coordinates: ' + (selectedBranch.latitude || '19.0') + '° N, ' + (selectedBranch.longitude || '72.8') + '° E · Min Reserve Threshold: ' + (selectedBranch.minThresholdPct || 15) + '%';
            document.getElementById('inspector-loan-book').textContent = money(selectedBranch.loanBook);
            document.getElementById('inspector-total-assets').textContent = money(selectedBranch.totalAssets);

            document.getElementById('inspector-ai-headline').textContent = 'Analyzing 10-day pattern...';
            document.getElementById('inspector-ai-reason').textContent = 'Fetching cash position and AI transaction diagnostics...';
            document.getElementById('inspector-ai-drivers').innerHTML = '<li>Loading factor indicators...</li>';
            document.getElementById('inspector-ai-action').textContent = 'Calculating recommended actions...';
            document.getElementById('inspector-daily-table').innerHTML = '<p class="muted-text" style="padding:12px;">Loading 10-day cash flow ledger...</p>';
            document.getElementById('inspector-transactions-list').innerHTML = '<p class="muted-text" style="padding:12px;">Loading transactions...</p>';

            Promise.all([
              apiClient.getPosition(branchId).catch(function () { return null; }),
              apiClient.getAiExplanation(branchId, 10).catch(function () { return null; }),
              apiClient.getDailyAnalysis(branchId, 10).catch(function () { return null; }),
              apiClient.getRecentTransactions(branchId).catch(function () { return []; })
            ]).then(function (results) {
              var position = results[0];
              var ai = results[1] || {};
              var analysis = normalizeAnalysis(results[2]);
              var txns = results[3] || [];

              if (position) {
                document.getElementById('inspector-reserve').textContent = money(position.currentReserve);
                var isSurplus = position.status === 'SURPLUS';
                document.getElementById('inspector-reserve-status').textContent = isSurplus ? 'Healthy Liquidity Buffer' : 'Below Required Threshold';
                document.getElementById('inspector-reserve-status').className = isSurplus ? 'flow-in' : 'flow-out';
                var delta = position.surplusOrDeficitAmount;
                document.getElementById('inspector-surplus-deficit').textContent = (Number(delta) >= 0 ? '+' : '') + money(delta);
                document.getElementById('inspector-surplus-deficit').className = isSurplus ? 'flow-in' : 'flow-out';
                document.getElementById('inspector-threshold-info').textContent = 'Min threshold ' + money(position.thresholdAmount) + ' (' + position.status + ')';
              }

              // AI Intelligence
              document.getElementById('inspector-ai-headline').textContent = ai.headline || 'Branch Liquidity Diagnosis';
              document.getElementById('inspector-ai-reason').textContent = ai.reason || 'Recent cash movements are being monitored.';
              document.getElementById('inspector-ai-action').textContent = ai.recommendedAction || 'Continue routine reserve monitoring.';
              document.getElementById('inspector-ai-source').textContent = ai.source || 'AI_ANALYTICS_ENGINE';
              if (ai.drivers && ai.drivers.length) {
                document.getElementById('inspector-ai-drivers').innerHTML = ai.drivers.map(function (d) { return '<li>' + d + '</li>'; }).join('');
              } else {
                document.getElementById('inspector-ai-drivers').innerHTML = '<li>Today net movement: ' + money(analysis.todayNet) + '</li><li>Average daily net: ' + money(analysis.averageDailyNet) + '</li>';
              }

              // 10-Day Cash Flow Breakdown Table
              var points = analysis.dailyPoints || [];
              if (points.length) {
                var tableHtml = '<div class="flow-row head"><span>Date</span><span>Deposits</span><span>Withdrawals</span><span>Net Impact</span><span class="flow-reserve">Closing Reserve</span></div>';
                tableHtml += points.map(function (pt) {
                  var net = Number(pt.netMovement) || 0;
                  var dep = Number(pt.deposits) || 0;
                  var wth = Number(pt.withdrawals) || 0;
                  var cls = Number(pt.closingReserve) || 0;
                  var netClass = net >= 0 ? 'flow-net pos' : 'flow-net neg';
                  return '<div class="flow-row">' +
                    '<span class="flow-date">' + (pt.date ? pt.date.slice(5) : '--') + '</span>' +
                    '<span class="flow-in">+' + money(dep) + '</span>' +
                    '<span class="flow-out">-' + money(wth) + '</span>' +
                    '<span class="' + netClass + '">' + (net >= 0 ? '+' : '') + money(net) + '</span>' +
                    '<span class="flow-reserve">' + money(cls) + '</span>' +
                    '</div>';
                }).join('');
                document.getElementById('inspector-daily-table').innerHTML = tableHtml;
              } else {
                document.getElementById('inspector-daily-table').innerHTML = '<p class="muted-text" style="padding:12px;">No 10-day history available for this branch.</p>';
              }

              // Transactions
              if (txns.length) {
                document.getElementById('inspector-transactions-list').innerHTML = txns.map(function (t) {
                  var isDep = t.txnType === 'DEPOSIT';
                  return '<div class="list-row">' +
                    '<span><strong class="' + (isDep ? 'flow-in' : 'flow-out') + '">' + t.txnType + '</strong><small>' + new Date(t.eventTimestamp).toLocaleString() + '</small></span>' +
                    '<strong class="' + (isDep ? 'flow-in' : 'flow-out') + '">' + (isDep ? '+' : '-') + money(t.amount) + '</strong>' +
                    '</div>';
                }).join('');
              } else {
                document.getElementById('inspector-transactions-list').innerHTML = '<p class="muted-text" style="padding:12px;">No recent transactions recorded.</p>';
              }
            });
          }

          function loadAdminOverview() {
            document.getElementById('admin-error').hidden = true;
            apiClient.getAdminOverview().then(function (overview) {
              cachedAdminBranches = overview.branches || [];
              document.getElementById('admin-cash').textContent = money(overview.totalCashReserve);
              document.getElementById('admin-loans').textContent = money(overview.totalLoanBook);
              document.getElementById('admin-assets').textContent = money(overview.totalAssets);

              // Query positions for all branches in parallel to compute network health
              var positionPromises = cachedAdminBranches.map(function (b) {
                return apiClient.getPosition(b.branchId).catch(function () { return { branchId: b.branchId, status: 'SURPLUS' }; });
              });

              return Promise.all(positionPromises).then(function (positions) {
                var positionMap = {};
                var surplusCount = 0;
                var deficitCount = 0;
                positions.forEach(function (pos) {
                  if (pos && pos.branchId) {
                    positionMap[pos.branchId] = pos;
                    if (pos.status === 'SURPLUS') surplusCount++;
                    else deficitCount++;
                  }
                });

                document.getElementById('admin-network-health').textContent = surplusCount + ' Surplus · ' + deficitCount + ' Deficit';
                document.getElementById('admin-network-sub').textContent = cachedAdminBranches.length + ' active branches monitored';

                var tableHtml = '<div class="admin-table-head"><span>Branch</span><span>Cash Reserve</span><span>Loan Book</span><span>Total Assets</span><span>Status</span><span>Action</span></div>';
                tableHtml += cachedAdminBranches.map(function (branch) {
                  var pos = positionMap[branch.branchId] || {};
                  var status = pos.status || 'SURPLUS';
                  var statusClass = status === 'SURPLUS' ? 'surplus' : 'deficit';
                  return '<div class="admin-row ' + (branch.branchId === currentAdminBranchId ? 'active' : '') + '" data-branch-id="' + branch.branchId + '">' +
                    '<span><strong>' + branch.branchName + '</strong><small>' + branch.branchId + '</small></span>' +
                    '<span>' + money(branch.currentReserve) + '<small>Min Thresh: ' + (branch.minThresholdPct || 15) + '%</small></span>' +
                    '<span>' + money(branch.loanBook) + '<small>Lending Book</small></span>' +
                    '<span>' + money(branch.totalAssets) + '<small>Total Assets</small></span>' +
                    '<span><span class="status-pill ' + statusClass + '">' + status + '</span></span>' +
                    '<span><button type="button" class="btn-inspect" data-inspect-branch="' + branch.branchId + '">Inspect</button></span>' +
                    '</div>';
                }).join('');
                document.getElementById('admin-branches').innerHTML = tableHtml;

                // Automatically inspect current branch or the first branch
                var targetBranchId = currentAdminBranchId || (cachedAdminBranches[0] ? cachedAdminBranches[0].branchId : null);
                if (targetBranchId) {
                  inspectBranch(targetBranchId);
                }
              });
            }).catch(function (err) {
              console.error(err);
              document.getElementById('admin-error').hidden = false;
            });
          }

          function showConsole(account, username) {
            if (account.role === 'BANK_ADMIN') {
              loginView.hidden = true;
              consoleView.hidden = true;
              adminView.hidden = false;
              loadAdminOverview();
              return;
            }
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
            adminView.hidden = true;
            loginView.hidden = false;
            loginForm.reset();
            window.clearInterval(window.branchHealthTimer);
            document.querySelectorAll('[data-panel]').forEach(function (panel) { panel.hidden = panel.dataset.panel !== 'overview'; });
            document.querySelectorAll('.tab-button').forEach(function (item) { item.classList.toggle('active', item.dataset.section === 'overview'); });
          });

          adminLogout.addEventListener('click', function () {
            sessionStorage.removeItem('branchCashSession');
            adminView.hidden = true;
            consoleView.hidden = true;
            loginView.hidden = false;
            loginForm.reset();
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

          var manualForm = document.getElementById('manual-cash-form');
          if (manualForm) {
            manualForm.addEventListener('submit', function (event) {
              event.preventDefault();
              var session = activeSession();
              if (!session || !session.role) return;
              var type = document.getElementById('manual-type').value;
              var amount = Number(document.getElementById('manual-amount').value);
              if (!amount || amount <= 0) {
                showToast('Enter a positive cash amount.', 'error');
                return;
              }
              apiClient.recordTransaction(session.role, type, amount).then(function () {
                showToast(type + ' of ' + money(amount) + ' recorded successfully.', 'success');
                document.getElementById('manual-amount').value = '';
                loadDashboard(accounts[session.username]);
              }).catch(function () {
                showToast('Cash movement could not be recorded.', 'error');
              });
            });
          }

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

          document.getElementById('admin-branches').addEventListener('click', function (event) {
            var row = event.target.closest('[data-branch-id]');
            if (row) {
              inspectBranch(row.getAttribute('data-branch-id'));
            }
          });

          document.getElementById('admin-inspector').addEventListener('click', function (event) {
            var btn = event.target.closest('[data-switch-branch]');
            if (btn) {
              inspectBranch(btn.getAttribute('data-switch-branch'));
            }
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
