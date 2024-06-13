package server.commands;

import common.Responce;
import common.requests.CountByMetersAboveSeaLevelRequest;
import common.requests.Request;
import common.requests.UpdateIdRequest;
import server.exceptions.CollectionIsEmptyException;
import server.exceptions.WrongAmountOfElementsException;
import server.managers.CollectionManager;

import server.utility.Console;

import java.io.IOException;
import java.util.Objects;

import static server.TCPServer.currentUserLogin;

public class UpdateId extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public UpdateId(Console console, CollectionManager collectionManager) {
        super("update id", "обновить значение элемента коллекции, id которого равен заданному");
        this.console = console;
        this.collectionManager = collectionManager;
    }


    public Responce applySp(UpdateIdRequest request) throws IOException {
        Responce responce = new Responce();
        try {

            if (collectionManager.collectionSize() == 0) throw new CollectionIsEmptyException();

            int key = request.getId();
            //System.out.println(currentUserLogin.get() + " " + collectionManager.getByKey(key).getCreatedBy());
            if (collectionManager.getByKey(key) != null){

                if (!collectionManager.getByKey(key).getCreatedBy().equals(currentUserLogin.get())){
                    responce.addString("Нет прав");
                } else {collectionManager.update(key, request.getCity());
                responce.addString("город успешно изменен");}

            } else {
                responce.addString("Нет города с таким id!");
            }

        } catch (CollectionIsEmptyException exception) {
            responce.addString("Коллекция пуста!");
        }
        return responce;
    }

        @Override
    public Responce apply(Request request) throws IOException {
        return applySp((UpdateIdRequest) request);
    }
}
