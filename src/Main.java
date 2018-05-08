
public class Main {
    public static void main(String[] args) {
        // Prints "Hello, World" to the terminal window.
        System.out.println("Hello, World");

        String link = "https://ogloszenia.trojmiasto.pl/praca-zatrudnie/slb,4,slwp,57.html";
        Bot bot = new Bot(link);
        bot.updateCache();
    }

}
