// Thin wrapper around fetch() for the Banking System REST API.
// Kept separate from app.js (UI logic) so the API contract is easy to
// find/reuse on its own, e.g. from a test script or another client.

const API_BASE = "http://localhost:8080/api";

function getAuthHeader() {
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;
  return "Basic " + btoa(`${username}:${password}`);
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "Authorization": getAuthHeader(),
      ...(options.headers || {})
    }
  });

  const statusDot = document.getElementById("auth-status");

  if (response.status === 401 || response.status === 403) {
    statusDot.className = "status-dot bad";
    throw new Error("Authentication failed - check username/password");
  }
  statusDot.className = "status-dot ok";

  if (response.status === 204) return null;

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = (data && data.message) ? data.message : `Request failed (${response.status})`;
    throw new Error(message);
  }
  return data;
}

const Api = {
  // Customers
  getCustomers: () => apiRequest("/customers"),
  createCustomer: (body) => apiRequest("/customers", { method: "POST", body: JSON.stringify(body) }),

  // Accounts
  getAccounts: () => apiRequest("/accounts"),
  openAccount: (body) => apiRequest("/accounts", { method: "POST", body: JSON.stringify(body) }),

  // Transactions
  deposit: (body) => apiRequest("/transactions/deposit", { method: "POST", body: JSON.stringify(body) }),
  withdraw: (body) => apiRequest("/transactions/withdraw", { method: "POST", body: JSON.stringify(body) }),
  transfer: (body) => apiRequest("/transactions/transfer", { method: "POST", body: JSON.stringify(body) }),
  getHistory: (accountNumber) => apiRequest(`/transactions/account/${accountNumber}`),
  getFlagged: () => apiRequest("/transactions/flagged"),

  // Reports
  getSummary: () => apiRequest("/reports/summary"),
  getAuditLog: () => apiRequest("/reports/audit-log"),
};
