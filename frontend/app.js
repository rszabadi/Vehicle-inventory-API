"use strict";

const STATUSES = ["AVAILABLE", "RESERVED", "SOLD"];
const DOC_TYPES = ["TECHNICAL_SHEET", "REGISTRATION_PERMIT", "ITV_CERTIFICATE", "SALES_CONTRACT"];

const euro = new Intl.NumberFormat("es-ES", {
  style: "currency", currency: "EUR", maximumFractionDigits: 0, useGrouping: "always",
});
const number = new Intl.NumberFormat("es-ES", { useGrouping: "always" });

const tbody = document.getElementById("vehicles");
const message = document.getElementById("message");
const emptyState = document.getElementById("empty");
const searchBox = document.getElementById("search");
const addForm = document.getElementById("add-form");
const dialog = document.getElementById("docs-dialog");
const docsList = document.getElementById("docs-list");
const docsMessage = document.getElementById("docs-message");
const docForm = document.getElementById("doc-form");
const docType = document.getElementById("doc-type");
const chips = document.querySelectorAll("[data-filter]");
const sortButtons = document.querySelectorAll("[data-sort]");

let vehicles = [];            // everything the API returned
let statusFilter = "";        // "" means all
let sort = { key: "id", dir: "desc" };   // newest first by default
let currentVehicle = null;    // the vehicle whose documents are open

// ---------- helpers ----------

function show(element, text, isError = false) {
  element.textContent = text;
  element.classList.toggle("error", isError);
}

// Text is always set with textContent, never innerHTML, so data from the API
// can never be interpreted as HTML (protects against XSS).
function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function button(label, onClick, className) {
  const b = el("button", className, label);
  b.type = "button";
  b.addEventListener("click", onClick);
  return b;
}

// TECHNICAL_SHEET -> "Technical sheet"
function pretty(value) {
  const words = value.toLowerCase().replaceAll("_", " ");
  return words.charAt(0).toUpperCase() + words.slice(1);
}

// A stable colour for each brand, so the same brand always looks the same.
function hueFor(text) {
  let hash = 0;
  for (const char of text.toLowerCase()) hash = (hash * 31 + char.charCodeAt(0)) % 360;
  return hash;
}

function csrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : "";
}

// Calls the API. Changes (POST/PUT/DELETE) carry the CSRF token; a 401 sends you to the login page.
async function api(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = { "Content-Type": "application/json", "Accept": "application/json" };
  if (method !== "GET") {
    headers["X-XSRF-TOKEN"] = csrfToken();
  }
  const response = await fetch(path, { ...options, headers });
  if (response.status === 401) {
    window.location.href = "/login";
    throw new Error("Please log in.");
  }
  if (!response.ok) {
    throw new Error(await errorText(response));
  }
  return response.status === 204 ? null : response.json();
}

async function errorText(response) {
  try {
    const problem = await response.json();
    if (problem.errors) {
      return Object.entries(problem.errors)
        .map(([field, reason]) => `${field}: ${reason}`)
        .join(", ");
    }
    return problem.detail || problem.title || `Error ${response.status}`;
  } catch {
    return `Error ${response.status}`;
  }
}

// ---------- vehicles: sorting, filtering, drawing ----------

const sorters = {
  id: (a, b) => a.id - b.id,
  brand: (a, b) => a.brand.localeCompare(b.brand, "es") || a.model.localeCompare(b.model, "es"),
  year: (a, b) => a.year - b.year,
  km: (a, b) => a.km - b.km,
  price: (a, b) => a.price - b.price,
  status: (a, b) => STATUSES.indexOf(a.status) - STATUSES.indexOf(b.status),
  documents: (a, b) => a.documentsReceived - b.documentsReceived,
};

function visibleVehicles() {
  const text = searchBox.value.trim().toLowerCase();
  const direction = sort.dir === "asc" ? 1 : -1;
  return vehicles
    .filter((v) => !statusFilter || v.status === statusFilter)
    .filter((v) => !text || `${v.brand} ${v.model}`.toLowerCase().includes(text))
    .sort((a, b) => sorters[sort.key](a, b) * direction);
}

function render() {
  const rows = visibleVehicles();
  tbody.replaceChildren(...rows.map(vehicleRow));

  emptyState.hidden = rows.length > 0;
  emptyState.textContent = vehicles.length === 0
    ? "The lot is empty. Add your first vehicle below."
    : "No vehicles match.";

  renderSummary();
  renderSortIndicators();
}

function renderSummary() {
  document.getElementById("count-all").textContent = vehicles.length;
  for (const status of STATUSES) {
    document.getElementById(`count-${status}`).textContent =
      vehicles.filter((v) => v.status === status).length;
  }
  const stock = vehicles
    .filter((v) => v.status === "AVAILABLE")
    .reduce((sum, v) => sum + v.price, 0);
  document.getElementById("stock-value").textContent = euro.format(stock);

  chips.forEach((chip) =>
    chip.setAttribute("aria-pressed", String(chip.dataset.filter === statusFilter)));
}

function renderSortIndicators() {
  sortButtons.forEach((b) => {
    const direction = sort.dir === "asc" ? "ascending" : "descending";
    b.closest("th").setAttribute("aria-sort", b.dataset.sort === sort.key ? direction : "none");
  });
}

function vehicleRow(v) {
  const tr = el("tr");

  const mono = el("span", "mono", v.brand.charAt(0).toUpperCase());
  mono.style.setProperty("--h", hueFor(v.brand));
  const name = el("span", "car-name");
  name.append(el("strong", "", v.brand), el("span", "model", v.model));
  const car = el("div", "car");
  car.append(mono, name);
  const carCell = el("td");
  carCell.append(car);

  const statusCell = el("td");
  statusCell.append(statusSelect(v));

  const meter = el("span", "meter");
  for (let i = 0; i < v.documentsTotal; i++) {
    meter.append(el("span", i < v.documentsReceived ? "seg on" : "seg"));
  }
  const docsButton = button("", () => openDocuments(v), "docs-link");
  docsButton.setAttribute("aria-label",
    `${v.documentsReceived} of ${v.documentsTotal} documents received for ${v.brand} ${v.model}. Open documents`);
  docsButton.append(meter, el("span", "docs-count", `${v.documentsReceived}/${v.documentsTotal}`));
  const docsCell = el("td");
  docsCell.append(docsButton);

  const actions = el("td", "actions");
  actions.append(button("Delete", () => removeVehicle(v), "danger"));

  tr.append(
    carCell,
    el("td", "num", v.year),
    el("td", "num", number.format(v.km)),
    el("td", "num price", euro.format(v.price)),
    statusCell,
    docsCell,
    actions,
  );
  return tr;
}

function statusSelect(v) {
  const select = el("select", "status");
  select.setAttribute("aria-label", `Status of ${v.brand} ${v.model}`);
  for (const value of STATUSES) {
    const option = el("option", "", pretty(value));
    option.value = value;
    option.selected = value === v.status;
    select.append(option);
  }
  select.dataset.value = v.status;
  select.addEventListener("change", () => {
    select.dataset.value = select.value;
    changeStatus(v, select.value);
  });
  return select;
}

async function loadVehicles() {
  try {
    vehicles = await api("/vehicles");
    render();
  } catch (e) {
    show(message, e.message, true);
  }
}

async function changeStatus(v, status) {
  try {
    // PUT replaces the whole vehicle, so we send all its fields with the new status.
    await api(`/vehicles/${v.id}`, {
      method: "PUT",
      body: JSON.stringify({
        brand: v.brand, model: v.model, year: v.year, km: v.km, price: v.price, status,
      }),
    });
    show(message, `${v.brand} ${v.model} is now ${pretty(status).toLowerCase()}.`);
  } catch (e) {
    show(message, e.message, true);
  }
  loadVehicles();
}

async function removeVehicle(v) {
  if (!confirm(`Delete ${v.brand} ${v.model}?`)) return;
  try {
    await api(`/vehicles/${v.id}`, { method: "DELETE" });
    show(message, "Vehicle deleted.");
  } catch (e) {
    show(message, e.message, true);
  }
  loadVehicles();
}

addForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = new FormData(addForm);
  const body = {
    brand: data.get("brand").trim(),
    model: data.get("model").trim(),
    year: Number(data.get("year")),
    km: Number(data.get("km")),
    price: Number(data.get("price")),
  };
  try {
    await api("/vehicles", { method: "POST", body: JSON.stringify(body) });
    addForm.reset();
    addForm.elements.brand.focus();
    show(message, "Vehicle added.");
    loadVehicles();
  } catch (e) {
    show(message, e.message, true);
  }
});

chips.forEach((chip) => chip.addEventListener("click", () => {
  statusFilter = chip.dataset.filter;
  render();
}));

sortButtons.forEach((b) => b.addEventListener("click", () => {
  const key = b.dataset.sort;
  sort = { key, dir: sort.key === key && sort.dir === "asc" ? "desc" : "asc" };
  render();
}));

searchBox.addEventListener("input", render);

// ---------- documents ----------

async function openDocuments(v) {
  currentVehicle = v;
  document.getElementById("docs-title").textContent = `Documents: ${v.brand} ${v.model}`;
  show(docsMessage, "");
  await loadDocuments();
  dialog.showModal();
}

async function loadDocuments() {
  try {
    const docs = await api(`/vehicles/${currentVehicle.id}/documents`);
    docsList.replaceChildren(...docs.map(docItem));
    if (docs.length === 0) {
      const li = el("li");
      li.append(el("span", "doc-meta", "No documents yet."));
      docsList.append(li);
    }
  } catch (e) {
    show(docsMessage, e.message, true);
  }
}

function docItem(d) {
  const li = el("li");
  li.append(
    el("span", "doc-name", pretty(d.type)),
    el("span", `tag tag-${d.status}`, pretty(d.status)),
  );
  if (d.expiresOn) li.append(el("span", "doc-meta", `expires ${d.expiresOn}`));

  const actions = el("span", "spacer");
  if (d.status === "PENDING") {
    actions.append(button("Mark received", () => markReceived(d)));
  }
  actions.append(button("Delete", () => removeDocument(d), "danger"));
  li.append(actions);
  return li;
}

async function markReceived(d) {
  try {
    await api(`/vehicles/${currentVehicle.id}/documents/${d.id}`, {
      method: "PUT",
      body: JSON.stringify({ status: "RECEIVED", expiresOn: d.expiresOn }),
    });
    show(docsMessage, "Marked as received.");
  } catch (e) {
    show(docsMessage, e.message, true);
  }
  loadDocuments();
}

async function removeDocument(d) {
  try {
    await api(`/vehicles/${currentVehicle.id}/documents/${d.id}`, { method: "DELETE" });
    show(docsMessage, "Document deleted.");
  } catch (e) {
    show(docsMessage, e.message, true);
  }
  loadDocuments();
}

docForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = new FormData(docForm);
  const body = { type: data.get("type"), expiresOn: data.get("expiresOn") || null };
  try {
    await api(`/vehicles/${currentVehicle.id}/documents`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    show(docsMessage, "Document added.");
    await loadDocuments();
  } catch (e) {
    show(docsMessage, e.message, true);
  }
});

// ---------- start ----------

for (const type of DOC_TYPES) {
  const option = el("option", "", pretty(type));
  option.value = type;
  docType.append(option);
}

document.getElementById("docs-close").addEventListener("click", () => dialog.close());

document.getElementById("logout").addEventListener("click", async () => {
  await fetch("/logout", { method: "POST", headers: { "X-XSRF-TOKEN": csrfToken() } });
  window.location.href = "/login?logout";
});

dialog.addEventListener("close", loadVehicles);

loadVehicles();
