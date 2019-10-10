package main;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.json.*;
import java.util.*;

public class MazeSolver {

    private static String mazeId;
    private static int height;
    private static int width;
    private static List<List<Integer>> path = new ArrayList<>();
    private static Set<String> visitedPositions = new HashSet<String>();

    public static void main(String[] args) throws IOException {
        getMaze();
        solve();
    }

    private static boolean isValidPosition(int xPos, int yPos) throws IOException {
        String url = "https://maze.coda.io/maze/" +  mazeId + "/check?x=" + xPos + "&y=" + yPos;
        URL urlForGetRequest = new URL(url);
        String readLine = null;
        HttpURLConnection connection = (HttpURLConnection) urlForGetRequest.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuffer response = new StringBuffer();
            while ((readLine = in.readLine()) != null) {
                response.append(readLine);
            } in.close();
            //System.out.println("JSON String Result " + response.toString());
            connection.disconnect();
            return true;
        } else {
            //System.out.println("GET NOT WORKING");
            return false;
        }
    }

    private static void getMaze() throws IOException {
        URL obj = new URL("https://maze.coda.io/maze");
        HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json");
        postConnection.setDoInput(true);
        postConnection.setDoOutput(true);
        int responseCode = postConnection.getResponseCode();
        System.out.println("POST Response Code :  " + responseCode);
        System.out.println("POST Response Message : " + postConnection.getResponseMessage());
        if (responseCode == HttpURLConnection.HTTP_CREATED) { //success
            JsonReader in = Json.createReader(new InputStreamReader(
                    postConnection.getInputStream()));
            JsonObject jsonObject = in.readObject();
            System.out.println(jsonObject.toString());
            mazeId = jsonObject.getString("id");
            width = jsonObject.getInt("width");
            height = jsonObject.getInt("height");
            in.close();
        } else {
            System.out.println("URL POST failed for get Maze");
        }
        postConnection.disconnect();
    }

    private static void solve() throws IOException{
        int xPos = 0;
        int yPos = 0;
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        if (traverse(xPos, yPos)){
            int length = path.size();
            if (length > 1){
                List<Integer> list = path.get(length - 1);
                if (list.contains(width - 1) && list.contains(height - 1)){
                    //construct json array
                    for(List<Integer> pos: path){
                        JsonObject jsonObject = Json.createObjectBuilder()
                                .add("x", pos.get(0))
                                .add("y", pos.get(1))
                                .build();
                        jsonArrayBuilder.add(jsonObject);
                    }
                } else {
                    path.clear();
                }
            }
        }
        JsonArray jsonArray = jsonArrayBuilder.build();

        //submit json array in POST
        URL url = new URL("https://maze.coda.io/maze/" + mazeId + "/solve");
        HttpURLConnection postConnection = (HttpURLConnection) url.openConnection();
        postConnection.setRequestMethod("POST");
        postConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        postConnection.setDoOutput(true);

        OutputStream os = postConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        writer.write(jsonArray.toString());
        writer.flush();
        writer.close();
        os.close();
        postConnection.connect();

        int responseCode = postConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) { //success
            if (!jsonArray.isEmpty()) {
                InputStream is = postConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                System.out.print("Response Code : " + responseCode);
            } else {
                System.out.print("Response Code : 422");
            }
        } else {
            System.out.println("Request failed to solve the maze, please retry");
        }
        System.out.println("\nSolving maze with id: "+ mazeId + "\nOrdered set of moves: " + jsonArray.toString());
        postConnection.disconnect();
    }

    private static boolean traverse (int x, int y) throws IOException {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;

        //if (x < 0 || y < 0) return false;
        if (!isValidPosition(x, y) || isVisited(x, y)) {
            return false;
        }
        path.add(Arrays.asList(x, y));
        visited(x, y);
        if (x == (width - 1) && y == (height - 1)) {
            return true;
        }
        if (traverse(x - 1, y)) {
            return true;
        }

        if (traverse(x + 1, y)) {
            return true;
        }

        if (traverse(x, y - 1)) {
            return true;
        }

        if (traverse(x, y + 1)) {
            return true;
        }
        path.remove(path.size() - 1);
        return false;
    }

    private static void visited(int x, int y){
        visitedPositions.add(Integer.toString(x) + "_" + Integer.toString(y));
    }

    private static boolean isVisited(int x, int y){
        return visitedPositions.contains(Integer.toString(x) + "_" + Integer.toString(y));
    }
}
