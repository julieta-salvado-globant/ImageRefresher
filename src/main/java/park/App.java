package park;

import eps.EPSClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Created by jsalvado on 02/08/16.
 */
public class App {
    public static void main(String[] args) {
        EPSClient eps = new EPSClient();

        String fileNameProfile = "/home/jsalvado/JAVA/profile.out";

        try (Stream<String> stream = Files.lines(Paths.get(fileNameProfile))) {
            stream.forEach(eps::update);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fileNameGroups = "/home/jsalvado/JAVA/group.out";

        try (Stream<String> stream = Files.lines(Paths.get(fileNameGroups))) {
            stream.forEach(eps::update);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fileNameItems = "/home/jsalvado/JAVA/item.out";

        try (Stream<String> stream = Files.lines(Paths.get(fileNameItems))) {
            stream.forEach(eps::update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
