// CloudPulse UI logic. Plain JavaScript, no framework.
// It only talks to our own Spring Boot API (/api/...), never to Prometheus directly.

const REFRESH_MS = 5000; // same interval as the server's monitoring tick

// ---------- Small helpers ----------

// Calls our API and returns the parsed JSON. Throws an Error with the server's message if the call failed.
async function api(path, method = "GET") {
    const response = await fetch("/api" + path, { method });
    if (!response.ok) {
        // Spring puts the reason (e.g. "Only OPEN incidents can be acknowledged") in the JSON "detail" field
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || body.message || `${method} ${path} failed (${response.status})`);
    }
    return response.json();
}

// Escapes text before inserting it as HTML, so data can never inject markup into the page.
function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

// "2026-10-13T16:02:53.61Z" -> "2026-10-13 16:02:53", or "—" when empty
function formatTime(iso) {
    return iso ? iso.replace("T", " ").substring(0, 19) : "—";
}

function badge(status) {
    return `<span class="badge ${esc(status)}">${esc(status)}</span>`;
}

function setStatusLine(text, isError = false) {
    const line = document.getElementById("status-line");
    line.textContent = text;
    line.className = isError ? "error" : "muted";
}

// ---------- Rendering ----------

async function loadServices() {
    // Fetch the services and the current simulation modes at the same time
    const [services, modes] = await Promise.all([api("/services"), api("/simulation")]);

    // For each service, fetch its newest metric reading (index 0 = newest)
    const latest = await Promise.all(services.map(s => api(`/services/${s.id}/metrics`).then(m => m[0])));

    const body = document.getElementById("services-body");
    if (services.length === 0) {
        body.innerHTML = `<tr><td colspan="5" class="muted">No services found.</td></tr>`;
        return;
    }
    body.innerHTML = services.map((s, i) => {
        const m = latest[i];
        return `<tr>
            <td>${esc(s.name)}</td>
            <td>${badge(s.status)}</td>
            <td>${esc(modes[s.name] || "NORMAL")}</td>
            <td>${m ? m.latencyMs.toFixed(0) + " ms" : "—"}</td>
            <td>${m ? (m.errorRate * 100).toFixed(1) + " %" : "—"}</td>
        </tr>`;
    }).join("");

    // Fill the dropdown once (rebuilding it every poll would reset the user's selection)
    const select = document.getElementById("service-select");
    if (select.options.length === 0) {
        select.innerHTML = services.map(s => `<option>${esc(s.name)}</option>`).join("");
    }
}

async function loadIncidents() {
    const incidents = await api("/incidents");
    const body = document.getElementById("incidents-body");
    if (incidents.length === 0) {
        body.innerHTML = `<tr><td colspan="8" class="muted">No incidents yet. Trigger a failure above.</td></tr>`;
        return;
    }
    body.innerHTML = incidents.map(inc => {
        // Only show the buttons that are valid for the current status
        const ackButton = inc.status === "OPEN"
            ? `<button data-incident="${inc.id}" data-op="acknowledge">Acknowledge</button>` : "";
        const resolveButton = inc.status !== "RESOLVED"
            ? `<button data-incident="${inc.id}" data-op="resolve">Resolve</button>` : "";
        return `<tr>
            <td>${inc.id}</td>
            <td>${esc(inc.alert.service.name)}</td>
            <td>${esc(inc.alert.ruleName)}</td>
            <td>${badge(inc.status)}</td>
            <td>${formatTime(inc.createdAt)}</td>
            <td>${formatTime(inc.acknowledgedAt)}</td>
            <td>${formatTime(inc.resolvedAt)}</td>
            <td>${ackButton} ${resolveButton}</td>
        </tr>`;
    }).join("");
}

// Reloads everything; called on a timer and after every button click.
async function refresh() {
    try {
        await Promise.all([loadServices(), loadIncidents()]);
        setStatusLine("Updated " + new Date().toLocaleTimeString());
    } catch (err) {
        setStatusLine("Could not reach CloudPulse API: " + err.message, true);
    }
}

// ---------- Button handling ----------

// Disables the button while the request runs, so a double-click cannot send the request twice.
async function runAction(button, path) {
    button.disabled = true;
    try {
        await api(path, "POST");
        await refresh();
    } catch (err) {
        setStatusLine(err.message, true);
    } finally {
        button.disabled = false;
    }
}

// One click listener for the whole page ("event delegation"): works for buttons created later too.
document.addEventListener("click", event => {
    const button = event.target.closest("button");
    if (!button) return;

    if (button.dataset.action === "restore") {
        runAction(button, "/simulation/restore");
    } else if (button.dataset.action) {
        const service = document.getElementById("service-select").value;
        runAction(button, `/simulation/${button.dataset.action}?service=${encodeURIComponent(service)}`);
    } else if (button.dataset.incident) {
        runAction(button, `/incidents/${button.dataset.incident}/${button.dataset.op}`);
    }
});

// Start: load once now, then poll every 5 seconds
refresh();
setInterval(refresh, REFRESH_MS);
