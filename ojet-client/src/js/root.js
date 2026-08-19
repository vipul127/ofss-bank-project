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
  function (Bootstrap, Context, ko, apiClient) {
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
            return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
          }

          function activeSession() {
            return JSON.parse(sessionStorage.getItem('branchCashSession'));
          }

          function loadDashboard(account) {
            var branchId = account.role;
            dashboardError.hidden = true;
            Promise.all([apiClient.getPosition(branchId), apiClient.getForecast(branchId), apiClient.getRecentTransactions(branchId), apiClient.getTransferRequests()])
              .then(function (results) {
                var position = results[0];
                var forecast = results[1];
                var transactions = results[2];
                var transfers = results[3];
                document.getElementById('reserve-value').textContent = money(position.currentReserve);
                document.getElementById('threshold-value').textContent = money(position.thresholdAmount);
                document.getElementById('status-value').textContent = position.status;
                document.getElementById('forecast-value').textContent = money(forecast.predictedPosition);
                document.getElementById('forecast-band').textContent = 'Range ' + money(forecast.confidenceBandLow) + ' to ' + money(forecast.confidenceBandHigh);
                document.getElementById('forecast-detail').innerHTML = '<strong>' + money(forecast.predictedPosition) + '</strong><p class="muted-text">Confidence band: ' + money(forecast.confidenceBandLow) + ' - ' + money(forecast.confidenceBandHigh) + '</p>';
                document.getElementById('transactions-list').innerHTML = transactions.length ? transactions.map(function (transaction) {
                  return '<div class="list-row"><span>' + transaction.txnType + '<small>' + new Date(transaction.eventTimestamp).toLocaleString() + '</small></span><strong>' + money(transaction.amount) + '</strong></div>';
                }).join('') : '<p class="muted-text">No transactions recorded yet.</p>';
                var ownTransfers = transfers.filter(function (transfer) { return transfer.destinationBranchId === branchId; });
                document.getElementById('transfers-list').innerHTML = ownTransfers.length ? ownTransfers.map(function (transfer) {
                  return '<div class="list-row"><span>From ' + transfer.sourceBranchId + '<small>' + transfer.requestId + '</small></span><strong>' + transfer.status + '</strong></div>';
                }).join('') : '<p class="muted-text">No transfer requests for this branch.</p>';
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
            loadDashboard(account);
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
