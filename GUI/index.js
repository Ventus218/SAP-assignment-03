const bikesDiv = document.getElementById('ebikes');
const usersDiv = document.getElementById('users');
const ridesDiv = document.getElementById('rides');
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
        const usersRequest = fetch('http://localhost:8082/users');
        const ridesRequest = fetch('http://localhost:8083/rides/active');
        const bikesResponse = await bikesRequest;
        const usersResponse = await usersRequest;
        const ridesResponse = await ridesRequest;


        if (bikesResponse.ok && ridesResponse.ok && usersResponse.ok) {
            return {
                error: false,
                bikes: await bikesResponse.json(),
                users: await usersResponse.json(),
                rides: await ridesResponse.json()
            }
        } else {
            var message = ""
            if (!bikesResponse.ok) {
                message += await bikesResponse.text() + "\n\n"
            }
            if (!usersResponse.ok) {
                message += await usersResponse.text() + "\n\n"
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
        const users = data.users
        const rides = data.rides
        rides.forEach(ride => {
            ride.eBike = bikes.find(b => b.id.value == ride.eBikeId.value)
        })

        bikesDiv.innerHTML = '';
        const bikesHeaderP = document.createElement('p');
        bikesHeaderP.textContent = "EBikes";
        bikesHeaderP.classList.add("fw-bold")
        bikesDiv.appendChild(bikesHeaderP)
        const addBikeButton = document.createElement("input")
        addBikeButton.type = "button"
        addBikeButton.value = "Add e-bike"
        addBikeButton.onmousedown = (e => {
            const id = prompt("Insert EBike id")            
            if (id) {
                const body = JSON.stringify({ "id": { "value": id } })
                fetch(
                    "http://localhost:8081/ebikes",
                    { method: 'POST', headers: { 'Content-type': 'application/json' }, body: body }
                );
            }
        })
        
        bikes.forEach(b =>{
            const p = document.createElement('p');
            p.textContent = `${b.id.value}`;
            bikesDiv.appendChild(p);
        })
        bikesDiv.appendChild(addBikeButton)
        
        usersDiv.innerHTML = '';
        const usersHeaderP = document.createElement('p');
        usersHeaderP.textContent = "Users";
        usersHeaderP.classList.add("fw-bold")
        usersDiv.appendChild(usersHeaderP)
        const addUserButton = document.createElement("input")
        addUserButton.type = "button"
        addUserButton.value = "Add user"
        addUserButton.onmousedown = (e => {
            const id = prompt("Insert User username")
            if (id) {
                const body = JSON.stringify({ value: id })
                fetch(
                    "http://localhost:8082/users",
                    { method: 'POST', headers: { 'Content-type': 'application/json' }, body: body}
                );
            }
        })
        
        users.forEach(u =>{
            const p = document.createElement('p');
            p.textContent = `${u.username.value}`;
            usersDiv.appendChild(p);
        })
        usersDiv.appendChild(addUserButton)

        ridesDiv.innerHTML = '';
        const ridesHeaderP = document.createElement('p');
        ridesHeaderP.textContent = "Rides";
        ridesHeaderP.classList.add("fw-bold")
        ridesHeaderP.classList.add("text-center")
        ridesDiv.appendChild(ridesHeaderP)

        const addRideButton = document.createElement("input")
        addRideButton.type = "button"
        addRideButton.value = "Start ride"
        addRideButton.onmousedown = (e => {
            const username = prompt("Insert user username")
            const junctionId = prompt("Insert user position (J1, J2, J3, J4, J5)")
            if (username && junctionId) {
                if (!users.find(u => u.username.value == username) || !["J1", "J2", "J3", "J4", "J5"].find(j => j == junctionId)) {
                    alert("Invalid username or junction")
                    return
                }
                const bikeId = prompt("Insert requested bike name")
                if (bikeId) {
                    if (!bikes.find(b => b.id.value == bikeId)) {
                        alert("Invalid bike id")
                        return
                    }
                    const body = JSON.stringify(
                        { 
                            eBikeId: { value: bikeId }, 
                            username: { value: username }, 
                            junctionId: { value: junctionId }
                        }
                    )
                    fetch(
                        "http://localhost:8083/rides",
                        { method: 'POST', headers: { 'Content-type': 'application/json' }, body: body}
                    );
                }
            }
        })
        const _buttonDiv = document.createElement("div")
        _buttonDiv.classList.add("col-12")
        _buttonDiv.classList.add("d-flex")
        _buttonDiv.classList.add("justify-content-center")
        _buttonDiv.appendChild(addRideButton)
        ridesDiv.appendChild(_buttonDiv)

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
                const button = document.createElement("input")
                button.type = "button"
                button.value = "Stop ride"
                button.onmousedown = (e => {
                    fetch(
                        "http://localhost:8083/rides/" + ride.id.value + "/userStoppedRiding",
                        { method: 'POST', headers: { 'Content-type': 'application/json' } }
                    );
                })
                div.appendChild(button)
            }
        });
    } else {
        displayError(data.message)
    }
}

setInterval(fetchDataAndUpdate, 50);

fetchDataAndUpdate();
