const select = document.getElementById('mealSelect');
const editSelect = document.getElementById('edit-mealName');
const dateField = document.getElementById('dateField');

const icons = {
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