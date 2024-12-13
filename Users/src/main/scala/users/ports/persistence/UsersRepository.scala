package users.ports.persistence;

import users.domain.model.*
import shared.ports.persistence.Repository;
import shared.ports.persistence.exceptions.*;

trait UsersRepository extends Repository[Username, User]
