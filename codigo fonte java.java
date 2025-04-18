import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class SistemaMonitoramentoAmbiental {
    private static SerialComunicator serialComunicator;
    
    public static void main(String[] args) {
        GerenciadorSensores gerenciador = new GerenciadorSensores();
        
        // Inicia comunicação com Arduino (ajuste a porta COM)
        serialComunicator = new SerialComunicator("COM3", 9600);
        
        // Configura observadores
        gerenciador.addObserver(new InterfaceUsuario());
        gerenciador.addObserver(new AlarmeHandler());
        gerenciador.addObserver(new LedHandler());
        
        // Simula recebimento de dados (substituir por leitura serial real)
        simularDados(gerenciador);
    }
    
    private static void simularDados(GerenciadorSensores gerenciador) {
        // Valores simulados - na prática viriam do Arduino
        gerenciador.atualizarDadosSensor(new DadosSensor("Temperatura", 25.0, "C"));
        gerenciador.atualizarDadosSensor(new DadosSensor("Umidade", 60.0, "%"));
        gerenciador.atualizarDadosSensor(new DadosSensor("Luminosidade", 500.0, "lx"));
    }
}

class SerialComunicator {
    public SerialComunicator(String porta, int baudRate) {
        System.out.println("Conectado ao Arduino na porta " + porta);
    }
    
    public void enviarComando(String comando) {
        System.out.println("[ARDUINO] Enviando: " + comando);
        // Implementação real enviaria via porta serial
    }
}

class DadosSensor {
    private String tipo;
    private double valor;
    private String unidade;
    
    public DadosSensor(String tipo, double valor, String unidade) {
        this.tipo = tipo;
        this.valor = valor;
        this.unidade = unidade;
    }
    
    // Getters...
}

class GerenciadorSensores extends Observable {
    private List<DadosSensor> dadosSensores = new ArrayList<>();
    
    public void atualizarDadosSensor(DadosSensor dados) {
        // Atualiza lista de sensores
        dadosSensores.removeIf(s -> s.getTipo().equals(dados.getTipo()));
        dadosSensores.add(dados);
        
        // Notifica observadores
        setChanged();
        notifyObservers(dados);
    }
    
    public List<DadosSensor> getTodosDados() {
        return new ArrayList<>(dadosSensores);
    }
}

class AlarmeHandler implements Observer {
    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof GerenciadorSensores && arg instanceof DadosSensor) {
            GerenciadorSensores gerenciador = (GerenciadorSensores) o;
            
            boolean condicaoCritica = gerenciador.getTodosDados().stream()
                .anyMatch(s -> (s.getTipo().equals("Temperatura") && s.getValor() > 40) ||
                               (s.getTipo().equals("Umidade") && s.getValor() < 20));
            
            if (condicaoCritica) {
                SistemaMonitoramentoAmbiental.serialComunicator.enviarComando("BUZZER_ON");
            } else {
                SistemaMonitoramentoAmbiental.serialComunicator.enviarComando("BUZZER_OFF");
            }
        }
    }
}

class LedHandler implements Observer {
    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof GerenciadorSensores && arg instanceof DadosSensor) {
            GerenciadorSensores gerenciador = (GerenciadorSensores) o;
            
            // Controle dos LEDs de temperatura
            gerenciador.getTodosDados().stream()
                .filter(s -> s.getTipo().equals("Temperatura"))
                .findFirst()
                .ifPresent(temp -> {
                    if (temp.getValor() > 40) {
                        SistemaMonitoramentoAmbiental.serialComunicator.enviarComando("TEMP_ALTA_ON");
                    } else if (temp.getValor() < 0) {
                        SistemaMonitoramentoAmbiental.serialComunicator.enviarComando("TEMP_BAIXA_ON");
                    } else {
                        SistemaMonitoramentoAmbiental.serialComunicator.enviarComando("TEMP_NORMAL");
                    }
                });
            
            // Controle do LED de alerta geral
            boolean alerta = gerenciador.getTodosDados().stream()
                .anyMatch(s -> (s.getTipo().equals("Temperatura") && s.getValor() > 40) ||
                               (s.getTipo().equals("Umidade") && s.getValor() < 20));
            
            SistemaMonitoramentoAmbiental.serialComunicator.enviarComando(
                alerta ? "ALERTA_ON" : "ALERTA_OFF");
            
            // Controle do LED de luminosidade
            gerenciador.getTodosDados().stream()
                .filter(s -> s.getTipo().equals("Luminosidade"))
                .findFirst()
                .ifPresent(luz -> {
                    SistemaMonitoramentoAmbiental.serialComunicator.enviarComando(
                        luz.getValor() > 700 ? "LUZ_OFF" : "LUZ_ON");
                });
        }
    }
}

class InterfaceUsuario implements Observer {
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof DadosSensor) {
            DadosSensor dados = (DadosSensor) arg;
            System.out.println("[Dados] " + dados.getTipo() + ": " + 
                             dados.getValor() + " " + dados.getUnidade());
        }
    }
}