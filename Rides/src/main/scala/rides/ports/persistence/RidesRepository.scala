package rides.ports.persistence;

import rides.domain.model.*
import shared.ports.persistence.Repository;
import shared.ports.persistence.exceptions.*;

trait RidesRepository extends Repository[RideId, Ride]
