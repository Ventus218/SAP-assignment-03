const ridesDiv = document.getElementById('rides');
const errorP = document.getElementById('error');
const stopRideButtons = {}

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

function rideStatusToText(rideStatus) {
    if (rideStatus.$type) {
        return `${rideStatus.$type} in ${rideStatus.junctionId.value}`
    } else {
        return rideStatus
    }
}

async function fetchDataAndUpdate() {
    const data = await fetchData()

    if (!data.error) {
        clearError();
        const bikes = data.bikes
        const rides = data.rides
        rides.forEach(ride => {
            ride.eBike = bikes.find(b => b.id.value == ride.eBikeId.value)
        })        

        ridesDiv.innerHTML = '';

        rides.forEach(ride => {
            const eBike = ride.eBike
            const div = document.createElement("div");
            div.classList.add("col")
            div.classList.add("text-center")
            ridesDiv.appendChild(div)

            const rideHeaderP = document.createElement('p');
            rideHeaderP.textContent += `${ride.eBikeId.value} -- ${ride.username.value}: ${rideStatusToText(ride.status)}`;
            rideHeaderP.classList.add("fw-bold")
            
            div.appendChild(rideHeaderP);

            const p = document.createElement('p');
            p.textContent += eBike.id.value + " is on ";
            p.textContent += eBike.location.$type.toLowerCase() + " " + eBike.location.id.value;
            div.appendChild(p);

            if (ride.status == "UserRiding") {
                var button = null
                if (!stopRideButtons[ride.id.value]) {
                    button = document.createElement("input")
                    stopRideButtons[ride.id.value] = button
                    button.type = "button"
                    button.value = "Stop ride"
                    button.onmousedown = (e => {
                        fetch(
                            "http://localhost:8083/rides/" + ride.id.value + "/userStoppedRiding",
                            { method: 'POST', headers: { 'Content-type': 'application/json' } }
                        );
                    })
                } else {
                    button = stopRideButtons[ride.id.value]
                }
                div.appendChild(button)
            }
        });
    } else {
        displayError(data.message)
    }
}

setInterval(fetchDataAndUpdate, 50);

fetchDataAndUpdate();
