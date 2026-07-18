// Thin wrapper around fetch() for the Banking System REST API.
// Credentials are captured once at login (see app.js handleLoginSubmit)
// and reused for every subsequent request via getAuthHeader().

const API_BASE = "https://zany-space-fortnight-jjgrq5p9q5773qxw5-8080.app.github.dev/api";

let currentCredentials = null; // { username, password }

function setCredentials(username, password) {
  currentCredentials = { username, password };
}

function clearCredentials() {
  currentCredentials = null;
}

function getAuthHeader() {
  if (!currentCredentials) return "";
  return "Basic " + btoa(`${currentCredentials.username}:${currentCredentials.password}`);
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

  if (response.status === 401 || response.status === 403) {
    throw new Error("Authentication failed - check username/password");
  }

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

  // Self-service (customer login)
  getMe: () => apiRequest("/me"),
  getMyAccounts: () => apiRequest("/me/accounts"),
  getMyTransactions: (accountNumber) => apiRequest(`/me/transactions/${accountNumber}`),
  registerCustomerLogin: (body) => apiRequest("/auth/register", { method: "POST", body: JSON.stringify(body) }),
};

Api.deleteCustomer = (id) => apiRequest(`/customers/${id}`, { method: "DELETE" });
