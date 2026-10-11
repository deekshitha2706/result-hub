document.addEventListener("DOMContentLoaded", () => {
    const filter = document.querySelector("[data-table-filter]");
    if (!filter) return;
    const rows = document.querySelectorAll("[data-filter-row]");
    filter.addEventListener("input", () => {
        const query = filter.value.trim().toLocaleLowerCase();
        rows.forEach(row => {
            row.hidden = !row.textContent.toLocaleLowerCase().includes(query);
        });
    });
});
