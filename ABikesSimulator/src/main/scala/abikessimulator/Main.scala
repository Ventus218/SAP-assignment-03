package abikessimulator

object Main extends App:
  val eBikesServiceAddress = sys.env.get("EBIKES_SERVICE_ADDRESS").get
  val ridesServiceAddress = sys.env.get("RIDES_SERVICE_ADDRESS").get
  val smartCityServiceAddress = sys.env.get("SMARTCITY_SERVICE_ADDRESS").get
  println("Starting ABikes simulator")
  ABikesSimulator(
    eBikesServiceAddress,
    ridesServiceAddress,
    smartCityServiceAddress
  ).execute()
