package service

import cats.effect.unsafe.implicits.global
import uz.scala.telegram.bot.{Commands, Polling, TelegramBot}

class TgBot(token: String, service: FindGameService) {
  val helloBot = new TelegramBot(token) with Polling with Commands
  helloBot.onCommand("find") { (sender, arg) =>
    helloBot.replyTo(sender) { service.findGame(arg.head).unsafeRunSync().head.price.toString }
  }
  helloBot.run()
}