package kartograffel.server.infrastructure.doobie.statements

import kartograffel.server.db.DbSpecification
import kartograffel.shared.domain.model.{User, Username}
import kartograffel.shared.model.ArbitraryInstances._
import kartograffel.shared.model.Id

class UserStatementsTest extends DbSpecification {
  check(UserStatements.findById(sampleOf[Id[User]]))

  check(UserStatements.findByName(sampleOf[Username]))
}