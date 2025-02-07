const bikesDiv = document.getElementById('bikes');
const errorP = document.getElementById('error');

function displayError(error) {
    errorP.textContent = "" + error;
}
function clearError() {
    errorP.textContent = "";
}

async function fetchData() {
    try {
        const bikesRequest = fetch('http://localhost:8081/ebikes');
        const ridesRequest = fetch('http://localhost:8083/rides/active');
        const bikesResponse = await bikesRequest;
        const ridesResponse = await ridesRequest;


        if (bikesResponse.ok && ridesResponse.ok) {
            return {
                error: false,
                bikes: await bikesResponse.json(),
                rides: await ridesResponse.json()
            }
        } else {
            var message = ""
            if (!bikesResponse.ok) {
                message += await bikesResponse.text() + "\n\n"
            }
            if (!ridesResponse.ok) {
                message += await ridesResponse.text()
            }
            return { error: true, message: message }
        }
    } catch (error) {
        return { error: true, message: "" + error }
    }
}

async function fetchDataAndUpdate() {
    const data = await fetchData()

    if (!data.error) {
        const bikes = data.bikes
        const rides = data.rides

        bikesDiv.innerHTML = '';

        bikes.forEach(eBike => {
            const p = document.createElement('p');
            p.textContent += eBike.id.value + " is on ";
            p.textContent += eBike.location.$type.toLowerCase() + " " + eBike.location.id.value;
            p.classList.add("col")
            p.classList.add("text-center")
            bikesDiv.appendChild(p);
        });
        errorP.textContent = '';
    } else {
        displayError(data.message)
    }
}

setInterval(fetchDataAndUpdate, 200);

fetchDataAndUpdate();
