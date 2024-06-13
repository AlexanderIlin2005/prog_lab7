package server.commands;
import common.Responce;
import common.requests.InsertRequest;
import common.requests.Request;
import server.exceptions.WrongAmountOfElementsException;
import server.managers.CollectionManager;
import client.utility.Checker;
import server.utility.Console;

import java.io.IOException;

import static server.TCPServer.currentUserLogin;

public class Insert extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public Insert(Console console, CollectionManager collectionManager) {
        super("insert  null", "добавить новый элемент с заданным ключом");
        this.console = console;
        this.collectionManager = collectionManager;

    }



    public Responce applySp(InsertRequest request) throws IOException {
        Responce responce = new Responce();

        int key = request.getKey();

        if (collectionManager.getByKey(key) != null){
            if (!collectionManager.getByKey(key).getCreatedBy().equals(currentUserLogin.get())){
                responce.addString("Нет прав");
            } else responce.addString("Город с таким id уже есть. Возможно, стоит использовать команду update_id");
        } else {
            this.collectionManager.insert(key, request.getCity());
            request.getCity().setCreator(currentUserLogin.get());
            //responce.addString("Город успешно добавлен.");
            responce.addString("Нет прав для доступа");
        }

        return responce;
    }

    @Override
    public Responce apply(Request request) throws IOException {
        Responce responce = applySp((InsertRequest) request);
        return responce;
    }
}
