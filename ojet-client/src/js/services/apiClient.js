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
      if (parsed.role) {
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
    getDailyAnalysis: function (branchId) {
      return request(serviceUrls.cash, '/api/cash-analysis/' + branchId + '?days=14');
    },
    getForecast: function (branchId) {
      return request(serviceUrls.forecast, '/api/forecast/' + branchId);
    },
    getTransferRequests: function () {
      return request(serviceUrls.requirement, '/api/transfer-requests');
    },
    getDeficitBranches: function () {
      return request(serviceUrls.requirement, '/api/cash-requirement/deficit-branches');
    },
    updateThreshold: function (branchId, minThresholdPct) {
      return request(serviceUrls.branch, '/api/branches/' + branchId + '/threshold', {
        method: 'PUT',
        body: JSON.stringify({ minThresholdPct: minThresholdPct })
      });
    }
  };
});
