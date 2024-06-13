package common.requests;

import java.io.Serializable;

public class Request implements Serializable {
    public final String name;

    public Request(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
