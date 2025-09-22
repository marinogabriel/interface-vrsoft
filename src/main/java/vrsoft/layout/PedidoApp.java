package vrsoft.layout;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import okhttp3.*;
import com.fasterxml.jackson.databind.*;
import org.jetbrains.annotations.NotNull;
import vrsoft.model.Pedido;

public class PedidoApp extends JFrame {
    private final JTextField produtoField;
    private final JTextField quantidadeField;
    private final JTextArea statusArea;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();

    private final Map<String, String> pedidosStatus = new ConcurrentHashMap<>();

    private static final String BASE_URL = "http://localhost:8080/api/pedidos";

    public PedidoApp() {
        super("Envio de Pedidos");

        // Layout básico
        produtoField = new JTextField(15);
        quantidadeField = new JTextField(5);
        JButton enviarButton = new JButton("Enviar Pedido");
        statusArea = new JTextArea(15, 40);
        statusArea.setEditable(false);

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Produto:"));
        inputPanel.add(produtoField);
        inputPanel.add(new JLabel("Quantidade:"));
        inputPanel.add(quantidadeField);
        inputPanel.add(enviarButton);

        this.setLayout(new BorderLayout());
        this.add(inputPanel, BorderLayout.NORTH);
        this.add(new JScrollPane(statusArea), BorderLayout.CENTER);

        enviarButton.addActionListener(e -> enviarPedido());

        // Inicia polling para atualizar status
        iniciarPollingStatus();

        this.pack();
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    private void enviarPedido() {
        String produto = produtoField.getText().trim();
        String quantidadeStr = quantidadeField.getText().trim();

        if (produto.isEmpty() || quantidadeStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha produto e quantidade!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantidade;
        try {
            quantidade = Integer.parseInt(quantidadeStr);
            if (quantidade <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade deve ser um número inteiro positivo!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Pedido pedido = new Pedido(produto, quantidade);

        // Serializa e envia pedido
        try {
            String json = objectMapper.writeValueAsString(pedido);
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException ex) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(PedidoApp.this, "Falha ao enviar pedido: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    if (response.isSuccessful()) {
                        // Atualiza status local e GUI
                        pedidosStatus.put(pedido.getId(), "ENVIADO, AGUARDANDO PROCESSO");
                        SwingUtilities.invokeLater(() -> {
                            atualizarAreaStatus();
                            produtoField.setText("");
                            quantidadeField.setText("");
                        });
                    } else {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(PedidoApp.this, "Erro no servidor: " + response.message(), "Erro", JOptionPane.ERROR_MESSAGE));
                    }
                    response.close();
                }
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao preparar pedido: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarAreaStatus() {
        StringBuilder sb = new StringBuilder();
        pedidosStatus.forEach((id, status) -> sb.append(id).append(": ").append(status).append("\n"));
        statusArea.setText(sb.toString());
    }

    private void iniciarPollingStatus() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            // Copia IDs que estão "AGUARDANDO PROCESSO"
            List<String> pendentes = new ArrayList<>();
            pedidosStatus.forEach((id, status) -> {
                if (status.contains("AGUARDANDO PROCESSO")) {
                    pendentes.add(id);
                }
            });

            for (String id : pendentes) {
                try {
                    Request request = new Request.Builder()
                            .url(BASE_URL + "/status/" + id)
                            .get()
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        String json = Objects.requireNonNull(response.body()).string();
                        // Supomos que backend retorna JSON {"status": "SUCESSO"} etc
                        Map<String, String> map = objectMapper.readValue(json, Map.class);
                        String novoStatus = map.get("status");

                        if ("SUCESSO".equalsIgnoreCase(novoStatus) || "FALHA".equalsIgnoreCase(novoStatus)) {
                            pedidosStatus.put(id, novoStatus);
                            SwingUtilities.invokeLater(this::atualizarAreaStatus);
                        }
                    }
                    response.close();
                } catch (Exception ex) {
                    // Pode logar ou ignorar erros de polling para não travar o app
                }
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PedidoApp().setVisible(true);
        });
    }
}
