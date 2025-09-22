O código nesse repositório representa a aplicação desktop em Java Swing, que devido ao limite de tempo e uma queda de energia, ficou extremamente simples, mas levemente funcional.

A aplicação consiste em apenas uma classe (fora a POJO para Produto), que contém toda a lógica e interface, uma prática ruim mas acabou ficando devido ao limite de tempo.

Tomei a liberdade de realizar uma pequena correção na verificação e atualização de status, que no commit original dentro da janela para realização do teste não funcionava.

Erro conhecido: erro na comunicação com o RabbitMQ, alguns pedidos podem ficar eternamente com o status "RECEBIDO", não atualizando para "SUCESSO"/"FALHA".
