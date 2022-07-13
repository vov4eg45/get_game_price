import uz.scala.telegram.bot.{Commands, Polling, TelegramBot}

object Bot extends App {
  val TOKEN = "5482996595:AAE3usr4G4rqhGLynNmRPoHX8l3UsbonlYk"
  val helloBot = new TelegramBot(TOKEN) with Polling with Commands
  helloBot.onCommand("hello") { (sender, args) =>
    helloBot.replyTo(sender) { s"Hello World! $args" }
  }
  helloBot.run()
}