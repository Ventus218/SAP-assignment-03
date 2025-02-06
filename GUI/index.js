const bikesDiv = document.getElementById('bikes');
const errorP = document.getElementById('error');

function displayError(error) {
    errorP.textContent = "" + error;
}

async function fetchDataAndUpdate() {
    try {
        const response = await fetch('http://localhost:8081/ebikes');
        if (response.ok) {
            const data = await response.json();
            console.log(data);


            bikesDiv.innerHTML = '';

            data.forEach(eBike => {
                const p = document.createElement('p');
                p.textContent += eBike.id.value + " is on ";
                p.textContent += eBike.location.$type.toLowerCase() + " " + eBike.location.id.value;
                p.classList.add("col")
                p.classList.add("text-center")
                bikesDiv.appendChild(p);
            });
            errorP.textContent = '';
        } else {
            displayError(await response.text())
        }
    } catch (error) {
        displayError(error)
    }
}

setInterval(fetchDataAndUpdate, 200);

fetchDataAndUpdate();
