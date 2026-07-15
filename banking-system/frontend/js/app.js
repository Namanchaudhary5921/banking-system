// UI logic: tab switching, form handling, table rendering.
// Talks to the backend only through the Api object defined in api.js.

document.addEventListener("DOMContentLoaded", () => {
  setupTabs();
  setupForms();
  loadSummary();
});

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
  const data = Object.fromEntries(new FormData(form).entries());
  return data;
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
      </tr>`).join("");
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
