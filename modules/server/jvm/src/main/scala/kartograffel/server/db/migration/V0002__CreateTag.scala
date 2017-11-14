package kartograffel.server.db.migration

import doobie._
import doobie.implicits._
import kartograffel.server.db.DoobieMigration

final class V0002__CreateTag extends DoobieMigration {
  override def migrate: ConnectionIO[_] =
    (sql"""
      CREATE TABLE tag (
        id BIGSERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        graffel_id BIGSERIAL REFERENCES graffel (id)
      )
    """: Fragment).update.run
}
