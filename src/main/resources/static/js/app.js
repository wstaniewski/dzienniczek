const select = document.getElementById('mealSelect');
const editSelect = document.getElementById('edit-mealName');
const dateField = document.getElementById('dateField');

const icons = {
    "Pierwsze śniadanie": "/icons/firstbreakfast.svg",
    "Śniadanie": "/icons/breakfast.svg",
    "Obiad": "/icons/lunch.svg",
    "Kolacja": "/icons/dinner.svg",
    "Przekąska": "/icons/snack.svg",
    "Inne": "/icons/other.svg"
};

function updateSelectIcon() {
    if (select) {
        select.style.backgroundImage = `url('${icons[select.value]}')`;
    }
}

function updateEditSelectIcon() {
    if (editSelect) {
        editSelect.style.backgroundImage = `url('${icons[editSelect.value]}')`;
    }
}

updateSelectIcon();
updateEditSelectIcon();

if (select) select.addEventListener('change', updateSelectIcon);
if (editSelect) editSelect.addEventListener('change', updateEditSelectIcon);

/* Ustawianie daty */
if (dateField) {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    dateField.value = `${yyyy}-${mm}-${dd}`;
}

function openEditFromButton(btn) {
    openEdit(
        btn.dataset.id,
        btn.dataset.meal,
        btn.dataset.calories,
        btn.dataset.date,
        btn.dataset.description || ""
    );
}

function openEdit(id, mealName, calories, date, description) {
    document.getElementById('edit-id').value = id;
    editSelect.value = mealName;
    updateEditSelectIcon();

    document.getElementById('edit-calories').value = calories;
    document.getElementById('edit-date').value = date;
    document.getElementById('edit-description').value = description;

    document.getElementById('edit-section').style.display = 'block';
}

function closeEdit() {
    document.getElementById('edit-section').style.display = 'none';
}

const fileInput = document.getElementById('fileUpload');
const fileNameSpan = document.getElementById('file-name');

if (fileInput) {
    fileInput.addEventListener('change', () => {
        const file = fileInput.files[0];
        fileNameSpan.textContent = file ? file.name : "";
    });
}

fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    fileNameSpan.textContent = file ? fileInput.value : "";
});

function showPremiumAlert(message, type = "success") {
    const alertBox = document.getElementById("import-alert");

    alertBox.innerHTML = `
        <span class="premium-alert-icon">${type === "success" ? "✔" : "✖"}</span>
        <span>${message}</span>
    `;

    alertBox.className = "premium-alert " +
        (type === "success" ? "premium-alert-success" : "premium-alert-error");

    alertBox.style.display = "flex";

    setTimeout(() => {
        alertBox.classList.add("premium-alert-show");
    }, 10);

    setTimeout(() => {
        alertBox.classList.remove("premium-alert-show");
        setTimeout(() => alertBox.style.display = "none", 400);
    }, 4000);
}

async function importProgman() {

    //const weeks = document.getElementById("weeksToImport").value;
    const fromDate = document.getElementById("importFromDate").value;
    const status = document.getElementById("progmanStatus");

    try {
        //const response = await fetch("/progman/import?weeks=" + weeks, {method: "POST"});
        const response = await fetch("/progman/import?fromDate=" + fromDate, {method: "POST"});
        const text = await response.text();
        console.log("STATUS:", response.status);
        console.log("ODPOWIEDZ:", text);
        status.innerHTML = response.status + " : " + text;
        window.location.href = "/";
    } catch (e) {
        console.error(e);
        status.innerHTML = "JS ERROR: " + e;
    }
}

async function resetEntries() {

    if (!confirm("Usunąć wszystkie wpisy?")) {
        return;
    }

    const response = await fetch("/reset", {
        method: "POST"
    });

    const text = await response.text();

    console.log(text);

    if (response.ok) {
        location.reload();
    }
}

document.addEventListener("DOMContentLoaded", () => {

    const fabMain = document.getElementById("fabMain");

    const fabMenu = document.getElementById("fabMenu");

    if (fabMain && fabMenu) {

        fabMain.addEventListener("click", () => {

            fabMenu.classList.toggle("open");

        });
    }
});

document.addEventListener("click", (e) => {

    const foodModal = document.getElementById("foodModal");

    const photoModal = document.getElementById("photoImportModal");

    const progmanModal = document.getElementById("progmanModal");

    if (e.target === foodModal) {
        closeFoodModal();
    }

    if (e.target === photoModal) {
        closePhotoImportModal();
    }

    if (e.target === progmanModal) {
        closeProgmanModal();
    }
});

function openFoodModal() {
    document.getElementById("foodModal").style.display = "flex";
    document.getElementById("pageContent").classList.add("blurred");
}

function closeFoodModal() {
    document.getElementById("foodModal").style.display = "none";
    document.getElementById("pageContent").classList.remove("blurred");
}

function openPhotoImportModal() {
    document.getElementById("photoImportModal").style.display = "flex";
    document.getElementById("pageContent").classList.add("blurred");
}

function closePhotoImportModal() {
    document.getElementById("photoImportModal").style.display = "none";
    document.getElementById("pageContent").classList.remove("blurred");
}

function openProgmanModal() {
    document.getElementById("progmanModal").style.display = "flex";
    document.getElementById("pageContent").classList.add("blurred");
}

function closeProgmanModal() {
    document.getElementById("progmanModal").style.display = "none";
    document.getElementById("pageContent").classList.remove("blurred");
}

function downloadPdf() {
    window.open("/pdf/food-entries", "_blank");
}