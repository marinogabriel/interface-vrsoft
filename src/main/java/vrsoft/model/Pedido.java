package vrsoft.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class Pedido implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String produto;
    private int quantidade;

    public Pedido() {}

    public Pedido(String produto, int quantidade) {
        this.id = UUID.randomUUID().toString();
        this.produto = produto;
        this.quantidade = quantidade;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("produto")
    public String getProduto() {
        return produto;
    }

    @JsonProperty("quantidade")
    public int getQuantidade() {
        return quantidade;
    }
}
