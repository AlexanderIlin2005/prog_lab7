package server.managers;

import BaseModel.*;
import common.Responce;
import server.utility.Console;


import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import java.util.concurrent.ConcurrentSkipListMap;


public class CollectionManager {

    private LocalDateTime lastInitTime;
    private LocalDateTime lastSaveTime;
    //private DumpManager dumpManager;

    private Console console;

    private Connection connection;


    private ConcurrentSkipListMap<Integer, City> collection = new ConcurrentSkipListMap<>();

    public CollectionManager(Console console, Connection connection) throws IOException {
        this.connection = connection;
        this.lastInitTime = null;
        this.lastSaveTime = null;
        //this.dumpManager = dumpManager;
        this.console = console;

    }

    public CollectionManager(){
    }

    public City minByPopulation() {
        City cityWithMinPopulation = collection.entrySet()
                .stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().getPopulation()))
                .map(Map.Entry::getValue)
                .orElse(null);
        return cityWithMinPopulation;
    }

    public long countByMetersAboveSeaLevel(Console console, Integer m){
        long count = collection.values().stream()
                .filter(city -> city.getMetersAboveSeaLevel() == m)
                .count();
        return count;
    }


    public ConcurrentSkipListMap<Integer, City> getCollection(){
        return collection;
    }




    public LocalDateTime getLastInitTime() {
        return lastInitTime;
    }


    public LocalDateTime getLastSaveTime() {
        return lastSaveTime;
    }



    public void clearCollection() {
        collection.clear();
    }

    public void validateAll(Console console) throws IOException {

        for(Map.Entry<Integer, City> entry : collection.entrySet()) {
            Integer key = entry.getKey();
            City city = entry.getValue();
            if (!city.validate()){
                console.printError("Элемент коллекции с id="+city.getId()+" имеет невалидные поля");
            }
        }

        console.println("Загруженные c объекты валидны.");
    }

    public boolean checkExist(int id) {
        for(Map.Entry<Integer, City> entry : collection.entrySet()) {
            Integer key = entry.getKey();
            City city = entry.getValue();
            if (city.getId() == id){return true;}
        }

        return false;
    }

    public String collectionType() {
        return collection.getClass().getName();
    }

    public int collectionSize() {
        return collection.size();
    }

    public City getFirst(){
        return (City) collection.firstEntry();
    }

    public City getLast(){
        return (City) collection.lastEntry();
    }

    public void addToCollection(City city){
        collection.put(City.getMaxId(), city);
        City.touchNextId();
    }

    public City getByKey(int key){
        for(Map.Entry<Integer, City> entry : collection.entrySet()) {
            Integer cityId = entry.getKey();
            City city = entry.getValue();
            if (cityId == key){return city;}
        }

        return null;
    }

    public void insert(Integer key, City city){
        collection.put(key, city);
        city.setId(key);
    }

    public void removeGreaterKey(Integer thresholdKey){
        collection.keySet().removeIf(key -> key > thresholdKey);

    }






    public void loadCollection() {
        ConcurrentSkipListMap<Integer, City> loadedCollection = new ConcurrentSkipListMap<>();

        String query = "SELECT * FROM city";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                City city = new City();
                city.setId(resultSet.getInt("id"));
                city.setName(resultSet.getString("name"));
                city.setCoordinates(new Coordinates(resultSet.getFloat("coordinates_x"), resultSet.getFloat("coordinates_y")));
                city.setCreationDate(resultSet.getString("creation_date"));
                city.setArea(resultSet.getLong("area"));
                city.setPopulation(resultSet.getLong("population"));
                city.setMetersAboveSeaLevel(resultSet.getInt("meters_above_sea_level"));
                city.setCapital(resultSet.getBoolean("capital"));
                city.setClimate(Climate.valueOf(resultSet.getString("climate")));
                city.setStandard0fLiving(StandardOfLiving.valueOf(resultSet.getString("standard_of_living")));
                city.setCreator(resultSet.getString("created_by"));
                Human governor = new Human(resultSet.getInt("governor_age"));
                city.setGovernor(governor);
                loadedCollection.put(city.getId(), city);
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Ошибка загрузки коллекции из базы данных: " + e.getMessage());
        }
        this.lastInitTime = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
        this.collection = loadedCollection;
        System.out.println("грузанули коллекцию из базы");

    }

    public void saveCollection() {
        String query = "INSERT INTO city (name, coordinates_x, coordinates_y, creation_date, area, population, meters_above_sea_level, capital, climate, standard_of_living, governor_age, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            //PreparedStatement clearStatement = connection.prepareStatement("TRUNCATE TABLE Collection");
            //clearStatement.executeUpdate();
            PreparedStatement statement = connection.prepareStatement(query);
            for (City city : collection.values()) {
                statement.setString(1, city.getName());
                statement.setFloat(2, city.getCoordinates().getX());
                statement.setFloat(3, city.getCoordinates().getY());
                statement.setString(4, city.getCreationDate());
                statement.setLong(5, city.getArea());
                statement.setLong(6, city.getPopulation());
                statement.setDouble(7, city.getMetersAboveSeaLevel());
                statement.setBoolean(8, city.getCapital());
                statement.setString(9, city.getClimate().name());
                statement.setString(10, city.getStandard0fLiving().name());
                statement.setInt(11, city.getGovernor().getAge());
                statement.setString(12, city.getCreatedBy());
                statement.executeUpdate();
                connection.commit();
            }
        } catch (SQLException e) {
            System.out.println("Ошибка сохранения коллекции в базу данных: " + e.getMessage());
        }
        this.lastSaveTime = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
    }

    @Override
    public String toString() {
        if (collection.isEmpty()) return "Коллекция пуста!";
        //var last = getLast();

        StringBuilder info = new StringBuilder();
        for(Map.Entry<Integer, City> entry : collection.entrySet()) {
            info.append(entry);
            info.append("\n\n");
        }
        return info.toString();
    }


    public void removeFromCollectionByKey(Integer key) {
        collection.remove(key);
    }

    public Responce groupCountingByArea(Responce responce) {
        TreeMap<Long, Integer> areaCountMap = new TreeMap<>();
        for (City city : collection.values()) {
            long area = city.getArea();
            areaCountMap.put(area, areaCountMap.getOrDefault(area, 0) + 1);
        }

        for (Long area : areaCountMap.keySet()) {
            responce.addString("Площадь: " + area + ", количество элементов: " + areaCountMap.get(area));
        }

        return responce;
    }

    public void update(Integer key, City city) {
        collection.put(key, city);
    }

    public void replaceIfGreater(Integer key, City city){
        if (collection.get(key).getPopulation() > city.getPopulation()){
            collection.put(key, city);
        }
    }


    public void replaceIfLower(Integer key, City city){
        if (collection.get(key).getPopulation() < city.getPopulation()){
            collection.put(key, city);
        }
    }
}
