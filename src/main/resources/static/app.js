// Calls the CloudPulse API and returns the JSON response
async function api(path, method = "GET") {
    const response = await fetch("/api" + path, { method });
    if (!response.ok) throw new Error(`${method} ${path} failed (${response.status})`);
    return response.json();
}

// "2026-10-13T16:02:53.61Z" -> "2026-10-13 16:02:53"
const time = iso => iso ? iso.replace("T", " ").substring(0, 19) : "—";
const badge = status => `<span class="badge ${status}">${status}</span>`;

function setStatusLine(text, isError) {
    const line = document.getElementById("status-line");
    line.textContent = text;
    line.className = isError ? "error" : "muted";
}

async function refresh() {
    try {
        const [services, modes, incidents] = await Promise.all([api("/services"), api("/simulation"), api("/incidents")]);
        // Newest metric reading for each service
        const latest = await Promise.all(services.map(s => api(`/services/${s.id}/metrics`)));

        document.getElementById("services-body").innerHTML = services.map((s, i) => `<tr>
            <td>${s.name}</td><td>${badge(s.status)}</td><td>${modes[s.name] || "normal"}</td>
            <td>${latest[i] ? latest[i].latencyMs.toFixed(0) + " ms" : "—"}</td>
            <td>${latest[i] ? (latest[i].errorRate * 100).toFixed(1) + " %" : "—"}</td></tr>`).join("");

        // Fill the dropdown only once, so refreshing doesn't reset the selection
        const select = document.getElementById("service-select");
        if (select.options.length === 0) select.innerHTML = services.map(s => `<option>${s.name}</option>`).join("");

        document.getElementById("incidents-body").innerHTML = incidents.map(inc => `<tr>
            <td>${inc.id}</td><td>${inc.alert.service.name}</td><td>${inc.alert.ruleName}</td><td>${badge(inc.status)}</td>
            <td>${time(inc.createdAt)}</td><td>${time(inc.acknowledgedAt)}</td><td>${time(inc.resolvedAt)}</td>
            <td>${inc.status === "OPEN" ? `<button data-incident="${inc.id}" data-op="acknowledge">Acknowledge</button>` : ""}
                ${inc.status !== "RESOLVED" ? `<button data-incident="${inc.id}" data-op="resolve">Resolve</button>` : ""}</td></tr>`).join("");

        setStatusLine("Updated " + new Date().toLocaleTimeString());
    } catch (err) {
        setStatusLine(err.message, true);
    }
}

// One click listener for every button on the page, including ones created later
document.addEventListener("click", async event => {
    const button = event.target.closest("button");
    if (!button) return;
    const service = document.getElementById("service-select").value;
    const path = button.dataset.action ? `/simulation/${button.dataset.action}?service=${service}`
        : `/incidents/${button.dataset.incident}/${button.dataset.op}`;
    try {
        await api(path, "POST");
        refresh();
    } catch (err) {
        setStatusLine(err.message, true);
    }
});

refresh();
setInterval(refresh, 5000);
