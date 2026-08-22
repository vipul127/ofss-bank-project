define([], function () {
  'use strict';

  var serviceUrls = {
    branch: 'http://localhost:8081',
    cash: 'http://localhost:8082',
    forecast: 'http://localhost:8083',
    requirement: 'http://localhost:8084'
  };

  function roleHeader() {
    var session = sessionStorage.getItem('branchCashSession');
    var headers = { 'Content-Type': 'application/json' };
    if (session) {
      var parsed = JSON.parse(session);
      if (parsed.role && parsed.role !== 'BANK_ADMIN') {
        headers['X-Branch-Role'] = parsed.role;
      }
    }
    return headers;
  }

  function request(baseUrl, path, options) {
    var requestOptions = options || {};
    requestOptions.headers = Object.assign(roleHeader(), requestOptions.headers || {});
    return fetch(baseUrl + path, requestOptions).then(function (response) {
      if (!response.ok) {
        throw new Error('API request failed: ' + response.status);
      }
      return response.status === 204 ? null : response.json();
    });
  }

  return {
    getBranch: function (branchId) {
      return request(serviceUrls.branch, '/api/branches/' + branchId);
    },
    getPosition: function (branchId) {
      return request(serviceUrls.cash, '/api/cash-position/' + branchId);
    },
    getRecentTransactions: function (branchId) {
      return request(serviceUrls.cash, '/api/cash-transaction/' + branchId + '/recent');
    },
    recordTransaction: function (branchId, txnType, amount) {
      return request(serviceUrls.cash, '/api/cash-transaction', {
        method: 'POST',
        body: JSON.stringify({ branchId: branchId, txnType: txnType, amount: amount })
      });
    },
    getDailyAnalysis: function (branchId, days) {
      return request(serviceUrls.cash, '/api/cash-analysis/' + branchId + '?days=' + (days || 10));
    },
    getAiExplanation: function (branchId, days) {
      return request(serviceUrls.cash, '/api/cash-analysis/' + branchId + '/explanation?days=' + (days || 10));
    },
    getForecast: function (branchId) {
      return request(serviceUrls.forecast, '/api/forecast/' + branchId);
    },
    getTransferRequests: function () {
      return request(serviceUrls.requirement, '/api/transfer-requests');
    },
    getNearbyBranches: function (branchId) {
      return request(serviceUrls.requirement, '/api/cash-requirement/nearby/' + branchId).catch(function () { return []; });
    },
    createTransferRequest: function (sourceBranchId, destinationBranchId, amount) {
      return request(serviceUrls.requirement, '/api/transfer-requests', {
        method: 'POST',
        body: JSON.stringify({ sourceBranchId: sourceBranchId, destinationBranchId: destinationBranchId, amount: amount })
      });
    },
    updateTransferStatus: function (requestId, status) {
      return request(serviceUrls.requirement, '/api/transfer-requests/' + requestId + '/status?status=' + status, { method: 'PUT' });
    },
    revokeTransferRequest: function (requestId) {
      return request(serviceUrls.requirement, '/api/transfer-requests/' + requestId, { method: 'DELETE' });
    },
    getDeficitBranches: function () {
      return request(serviceUrls.requirement, '/api/cash-requirement/deficit-branches');
    },
    updateThreshold: function (branchId, minThresholdPct) {
      return request(serviceUrls.branch, '/api/branches/' + branchId + '/threshold', {
        method: 'PUT',
        body: JSON.stringify({ minThresholdPct: minThresholdPct })
      });
    },
    getAdminOverview: function () {
      // Real aggregate from branch-service's own AdminController — includes actual
      // loanBook/totalAssets per branch, not client-synthesized stand-ins.
      return request(serviceUrls.branch, '/api/admin/overview');
    },
    getAdminInsights: function () {
      return request(serviceUrls.cash, '/api/admin/insights');
    }
  };
});
