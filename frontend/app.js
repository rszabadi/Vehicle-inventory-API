"use strict";

const STATUSES = ["AVAILABLE", "RESERVED", "SOLD"];
const DOC_TYPES = ["TECHNICAL_SHEET", "REGISTRATION_PERMIT", "ITV_CERTIFICATE", "SALES_CONTRACT"];

const tbody = document.getElementById("vehicles");
const filter = document.getElementById("filter");
const message = document.getElementById("message");
const addForm = document.getElementById("add-form");
const dialog = document.getElementById("docs-dialog");
const docsList = document.getElementById("docs-list");
const docsMessage = document.getElementById("docs-message");
const docForm = document.getElementById("doc-form");
const docType = document.getElementById("doc-type");

let currentVehicle = null;

// ---------- helpers ----------

function show(element, text, isError = false) {
  element.textContent = text;
  element.classList.toggle("error", isError);
}

// Calls the API and turns error responses (problem format) into readable messages.
async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
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

// Text is always set with textContent, never innerHTML, so data from the API
// can never be interpreted as HTML (protects against XSS).
function cell(text) {
  const td = document.createElement("td");
  td.textContent = text;
  return td;
}

function button(label, onClick) {
  const b = document.createElement("button");
  b.type = "button";
  b.textContent = label;
  b.addEventListener("click", onClick);
  return b;
}

function select(options, current, onChange) {
  const s = document.createElement("select");
  for (const value of options) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = value;
    option.selected = value === current;
    s.append(option);
  }
  s.addEventListener("change", () => onChange(s.value));
  return s;
}

// ---------- vehicles ----------

async function loadVehicles() {
  try {
    const query = filter.value ? `?status=${encodeURIComponent(filter.value)}` : "";
    const vehicles = await api(`/vehicles${query}`);
    tbody.replaceChildren(...vehicles.map(vehicleRow));
    if (vehicles.length === 0) {
      show(message, "No vehicles found.");
    }
  } catch (e) {
    show(message, e.message, true);
  }
}

function vehicleRow(v) {
  const tr = document.createElement("tr");
  tr.append(cell(v.brand), cell(v.model), cell(v.year), cell(v.km), cell(v.price));

  const statusCell = document.createElement("td");
  statusCell.append(select(STATUSES, v.status, (status) => changeStatus(v, status)));

  const actions = document.createElement("td");
  actions.append(
    button("Documents", () => openDocuments(v)),
    button("Delete", () => removeVehicle(v)),
  );

  tr.append(statusCell, actions);
  return tr;
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
    show(message, `Vehicle ${v.id} is now ${status}.`);
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
    show(message, "Vehicle added.");
    loadVehicles();
  } catch (e) {
    show(message, e.message, true);
  }
});

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
      const li = document.createElement("li");
      li.textContent = "No documents yet.";
      docsList.append(li);
    }
  } catch (e) {
    show(docsMessage, e.message, true);
  }
}

function docItem(d) {
  const li = document.createElement("li");
  const expires = d.expiresOn ? `, expires ${d.expiresOn}` : "";
  const label = document.createElement("span");
  label.textContent = `${d.type} (${d.status}${expires}) `;
  li.append(label);
  if (d.status === "PENDING") {
    li.append(button("Mark received", () => markReceived(d)));
  }
  li.append(button("Delete", () => removeDocument(d)));
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
  const option = document.createElement("option");
  option.value = type;
  option.textContent = type;
  docType.append(option);
}

filter.addEventListener("change", () => {
  show(message, "");
  loadVehicles();
});
document.getElementById("docs-close").addEventListener("click", () => dialog.close());

loadVehicles();
