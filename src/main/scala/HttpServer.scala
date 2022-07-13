import DB.DAO.DAOImpl
import DB.XMigration
import cats.data.Kleisli
import cats.effect._
import com.comcast.ip4s._
import doobie.hikari._
import doobie.implicits._
import doobie.util.ExecutionContexts
import io.circe.generic.auto._
import logic.{MvideoLogic, PsLogic, XboxLogic}
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._
import org.http4s.ember.server._
import org.http4s.server.Server
import org.http4s.{HttpRoutes, Request, Response}
import service.FindGameService

object HttpServer extends IOApp {

  def transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql:postgres",
        "postgres",
        "123",
        ce
      )
    } yield xa

  def findGameService(implicit xa: doobie.Transactor[IO]): FindGameService = {
    val dao = new DAOImpl
    val mvideoLogic = new MvideoLogic(dao)
    val psLogic = new PsLogic(dao)
    val xlogic = new XboxLogic(dao)
    new FindGameService(mvideoLogic, psLogic, xlogic, dao)
  }

  def initialization(implicit xa: doobie.Transactor[IO]): IO[Unit] = {
    for {
      _ <- XMigration.beforeAll.transact(xa)
      _ <- findGameService.writeToDB
    } yield ()
  }

  def route(implicit xa: doobie.Transactor[IO], service: FindGameService): Kleisli[IO, Request[IO], Response[IO]] = {
    object TitleQueryParameterMatcher extends OptionalQueryParamDecoderMatcher[String]("title")
    HttpRoutes.of[IO] {
      case GET -> Root / "find" :? TitleQueryParameterMatcher(title) =>
        title match {
          case Some(value) => Ok(service.findGame(value))
          case None => Ok("Nothing")
        }
      case GET -> Root / "find" / "vendor"/ vendor =>
        Ok(s"Find $vendor")
      case GET -> Root / "subscribe" / id =>
        Ok(s"Subscribed on $id")
    }.orNotFound
  }

  def server(route: Kleisli[IO, Request[IO], Response[IO]]) = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(route)
      .build
  }

  def resource: Resource[IO, Server] = {
    //println("started")
    for {
      xa <- transactor
      r = route(xa, findGameService(xa))
      srv <- server(r)
    } yield srv
  }

  val TOKEN = "5482996595:AAE3usr4G4rqhGLynNmRPoHX8l3UsbonlYk"

  def tgBot = {
    ???
  }

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      //_ <- transactor.use(xa => initialization(xa))
      a <- resource.use(_ => IO.never.as(ExitCode.Success))
    } yield a
  }

}