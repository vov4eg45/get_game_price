package DB

import cats.free.Free
import doobie.free.connection
import doobie.implicits.toSqlInterpolator

object XMigration {

  val dropGameTable: doobie.ConnectionIO[Int] = {
    println("droped")
    sql"DROP TABLE IF EXISTS gameInfo".update.run
  }

  val createGameTable: doobie.ConnectionIO[Int] = {
    println("created")
    sql"""
      CREATE TABLE gameInfo(
      title       VARCHAR(150)     NOT NULL,
      platforms    VARCHAR []      NOT NULL,
      price       NUMERIC(5,2)    NOT NULL,
      source      VARCHAR(20)     NOT NULL
      );
       """.update.run
  }

  val createWatchListTable: doobie.ConnectionIO[Int] =
    sql"""
         CREATE TABLE watchList(
         id INTEGER NOT NULL,
       """.update.run

  val beforeAll: Free[connection.ConnectionOp, Unit] = for {
    _ <- XMigration.dropGameTable
    _ <- XMigration.createGameTable
  } yield ()
}