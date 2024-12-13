package ebikes.ports.persistence;

import ebikes.domain.model.*
import shared.ports.persistence.Repository;
import shared.ports.persistence.exceptions.*;

trait EBikesRepository extends Repository[EBikeId, EBike]
