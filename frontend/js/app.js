// UI logic: login gate, tab switching, form handling, table rendering.
// Talks to the backend only through the Api object defined in api.js.

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("login-submit-btn").addEventListener("click", handleLoginSubmit);
  document.getElementById("password").addEventListener("keydown", (e) => {
    if (e.key === "Enter") handleLoginSubmit();
  });
  document.getElementById("logout-btn").addEventListener("click", handleLogout);

  setupTabs();
  setupForms();
});

// ---------- Login ----------
async function handleLoginSubmit() {
  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value;
  const errorBox = document.getElementById("login-error");
  errorBox.textContent = "";

  if (!username || !password) {
    errorBox.textContent = "Enter both username and password.";
    return;
  }

  setCredentials(username, password);

  try {
    const me = await Api.getMe();
    document.getElementById("login-screen").style.display = "none";
    document.getElementById("app-shell").style.display = "block";
    document.getElementById("logged-in-as").textContent = `${me.username} (${me.role})`;
    applyRoleUI(me.role);
  } catch (err) {
    clearCredentials();
    errorBox.textContent = "Invalid username or password.";
  }
}

function handleLogout() {
  clearCredentials();
  document.getElementById("app-shell").style.display = "none";
  document.getElementById("login-screen").style.display = "flex";
  document.getElementById("username").value = "";
  document.getElementById("password").value = "";
  document.getElementById("login-error").textContent = "";
}

function applyRoleUI(role) {
  const staffTabs = ["dashboard", "customers", "accounts", "transactions", "reports"];
  const myAccountBtn = document.querySelector('[data-tab="myaccount"]');

  document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
  document.querySelectorAll(".tab-panel").forEach(p => p.classList.remove("active"));

  if (role === "CUSTOMER") {
    staffTabs.forEach(t => {
      const btn = document.querySelector(`[data-tab="${t}"]`);
      if (btn) btn.style.display = "none";
    });
    myAccountBtn.style.display = "inline-block";
    myAccountBtn.classList.add("active");
    document.getElementById("myaccount").classList.add("active");
    loadMyAccounts();
  } else {
    staffTabs.forEach(t => {
      const btn = document.querySelector(`[data-tab="${t}"]`);
      if (btn) btn.style.display = "inline-block";
    });
    myAccountBtn.style.display = "none";
    document.querySelector('[data-tab="dashboard"]').classList.add("active");
    document.getElementById("dashboard").classList.add("active");
    loadSummary();
  }
}

// ---------- Tabs ----------
function setupTabs() {
  document.querySelectorAll(".tab-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
      document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
      btn.classList.add("active");
      document.getElementById(btn.dataset.tab).classList.add("active");

      if (btn.dataset.tab === "customers") loadCustomers();
      if (btn.dataset.tab === "accounts") loadAccounts();
      if (btn.dataset.tab === "reports") { loadFlagged(); loadAuditLog(); }
      if (btn.dataset.tab === "myaccount") loadMyAccounts();
    });
  });
}

// ---------- Toast ----------
function showToast(message, isError = false) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.className = "toast show" + (isError ? " error" : "");
  setTimeout(() => { toast.className = "toast"; }, 3500);
}

function formToJson(form) {
  return Object.fromEntries(new FormData(form).entries());
}

// ---------- Forms ----------
function setupForms() {
  document.getElementById("customer-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await Api.createCustomer(formToJson(e.target));
      showToast("Customer onboarded successfully");
      e.target.reset();
      loadCustomers();
    } catch (err) { showToast(err.message, true); }
  });

  const createLoginForm = document.getElementById("create-login-form");
  createLoginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      const body = formToJson(e.target);
      body.customerId = Number(body.customerId);
      await Api.registerCustomerLogin(body);
      showToast("Customer login created");
      e.target.reset();
    } catch (err) { showToast(err.message, true); }
  });

  document.getElementById("account-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      const body = formToJson(e.target);
      body.customerId = Number(body.customerId);
      body.openingBalance = Number(body.openingBalance || 0);
      await Api.openAccount(body);
      showToast("Account opened successfully");
      e.target.reset();
      loadAccounts();
    } catch (err) { showToast(err.message, true); }
  });

  document.getElementById("deposit-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      const body = formToJson(e.target);
      body.amount = Number(body.amount);
      await Api.deposit(body);
      showToast("Deposit successful");
      e.target.reset();
    } catch (err) { showToast(err.message, true); }
  });

  document.getElementById("withdraw-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      const body = formToJson(e.target);
      body.amount = Number(body.amount);
      await Api.withdraw(body);
      showToast("Withdrawal successful");
      e.target.reset();
    } catch (err) { showToast(err.message, true); }
  });

  document.getElementById("transfer-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      const body = formToJson(e.target);
      body.amount = Number(body.amount);
      await Api.transfer(body);
      showToast("Transfer completed");
      e.target.reset();
    } catch (err) { showToast(err.message, true); }
  });
}

// ---------- Loaders ----------
async function loadSummary() {
  try {
    const s = await Api.getSummary();
    document.getElementById("stat-customers").textContent = s.totalCustomers;
    document.getElementById("stat-accounts").textContent = s.totalAccounts;
    document.getElementById("stat-assets").textContent = "$" + Number(s.totalAssetsUnderManagement).toLocaleString();
    document.getElementById("stat-flagged").textContent = s.flaggedTransactionCount;
  } catch (err) { showToast(err.message, true); }
}

async function loadCustomers() {
  try {
    const customers = await Api.getCustomers();
    const tbody = document.getElementById("customer-table-body");
    tbody.innerHTML = customers.map(c => `
      <tr>
        <td>${c.id}</td>
        <td>${c.firstName} ${c.lastName}</td>
        <td>${c.email}</td>
        <td>${c.nationalId}</td>
        <td>${c.onboardedDate}</td>
        <td><button class="btn" style="padding:5px 10px;font-size:0.8rem;" onclick="removeCustomer(${c.id})">Remove</button></td>
      </tr>`).join("");
  } catch (err) { showToast(err.message, true); }
}

async function removeCustomer(id) {
  if (!confirm(`Remove customer #${id}? This cannot be undone.`)) return;
  try {
    await Api.deleteCustomer(id);
    showToast("Customer removed");
    loadCustomers();
  } catch (err) { showToast(err.message, true); }
}

async function loadAccounts() {
  try {
    const accounts = await Api.getAccounts();
    const tbody = document.getElementById("account-table-body");
    tbody.innerHTML = accounts.map(a => `
      <tr>
        <td>${a.accountNumber}</td>
        <td>${a.customer ? a.customer.id : ""}</td>
        <td>${a.accountType}</td>
        <td>$${Number(a.balance).toFixed(2)}</td>
        <td>${(Number(a.interestRate) * 100).toFixed(2)}%</td>
        <td>${a.active ? "Yes" : "No"}</td>
      </tr>`).join("");
  } catch (err) { showToast(err.message, true); }
}

async function loadHistory() {
  const accountNumber = document.getElementById("history-account").value.trim();
  if (!accountNumber) { showToast("Enter an account number", true); return; }
  try {
    const txns = await Api.getHistory(accountNumber);
    const tbody = document.getElementById("transaction-table-body");
    tbody.innerHTML = txns.map(t => `
      <tr class="${t.flagged ? "flagged-row" : ""}">
        <td>${new Date(t.timestamp).toLocaleString()}</td>
        <td>${t.type}</td>
        <td>$${Number(t.amount).toFixed(2)}</td>
        <td>$${Number(t.balanceAfter).toFixed(2)}</td>
        <td><span class="badge ${t.flagged ? "yes" : "no"}">${t.flagged ? "Flagged" : "Clear"}</span></td>
      </tr>`).join("");
  } catch (err) { showToast(err.message, true); }
}

async function loadFlagged() {
  try {
    const txns = await Api.getFlagged();
    const tbody = document.getElementById("flagged-table-body");
    tbody.innerHTML = txns.map(t => `
      <tr>
        <td>${new Date(t.timestamp).toLocaleString()}</td>
        <td>${t.account ? t.account.accountNumber : ""}</td>
        <td>${t.type}</td>
        <td>$${Number(t.amount).toFixed(2)}</td>
        <td>${t.flagReason || ""}</td>
      </tr>`).join("");
  } catch (err) { showToast(err.message, true); }
}

async function loadAuditLog() {
  try {
    const logs = await Api.getAuditLog();
    const tbody = document.getElementById("audit-table-body");
    tbody.innerHTML = logs.map(l => `
      <tr>
        <td>${new Date(l.timestamp).toLocaleString()}</td>
        <td>${l.entityType} #${l.entityId}</td>
        <td>${l.action}</td>
        <td>${l.details || ""}</td>
        <td>${l.performedBy}</td>
      </tr>`).join("");
  } catch (err) { showToast(err.message, true); }
}

async function loadMyAccounts() {
  try {
    const accounts = await Api.getMyAccounts();
    const container = document.getElementById("my-accounts-list");
    container.innerHTML = accounts.map(a => `
      <div class="card" style="margin-bottom:10px;">
        <span class="card-label">${a.accountType} - ${a.accountNumber}</span>
        <span class="card-value">$${Number(a.balance).toFixed(2)}</span>
        <button class="btn" style="margin-top:8px;width:fit-content;" onclick="loadMyTransactions('${a.accountNumber}')">View Transactions</button>
      </div>`).join("");
  } catch (err) { showToast(err.message, true); }
}

async function loadMyTransactions(accountNumber) {
  try {
    const txns = await Api.getMyTransactions(accountNumber);
    const tbody = document.getElementById("my-transactions-body");
    tbody.innerHTML = txns.map(t => `
      <tr><td>${new Date(t.timestamp).toLocaleString()}</td><td>${t.type}</td>
      <td>$${Number(t.amount).toFixed(2)}</td><td>$${Number(t.balanceAfter).toFixed(2)}</td></tr>`).join("");
  } catch (err) { showToast(err.message, true); }
}
