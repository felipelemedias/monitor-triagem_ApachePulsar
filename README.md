# Sistema de Monitoramento de Sinais Vitais com Apache Pulsar

Um sistema de monitoramento de pacientes em tempo real utilizando Apache Pulsar como middleware de mensagens, projetado para ambientes clínicos com o objetivo de rastrear sinais vitais e gerar alertas.

## Funcionalidades

- **Monitoramento em tempo real** dos sinais vitais:
  - Frequência cardíaca (FC)
  - Frequência respiratória (FR)
  - Temperatura
  - Pressão arterial
- **Sistema inteligente de alertas** com:
  - Notificações por prioridade
  - Confirmação por profissionais da saúde
  - Escalonamento automático
- **Persistência dos dados**:
  - Registros históricos em CSV
  - Logs de alertas para auditoria
- **Arquitetura distribuída**:
  - Vários clientes concorrentes
  - Servidor escalável
  - Projeto tolerante a falhas

## Stack Tecnológico

- **Backend**: Java 11
- **Middleware**: Apache Pulsar 2.11
- **Formato de Dados**: Mensagens tipo JSON (em String)
- **Armazenamento**:
  - Arquivos CSV para dados históricos
  - Arquivos de log para alertas

## Pré-Requisitos

- Java JDK 11+
- Apache Pulsar 2.11 em execução local

```bash
docker run -it -p 6650:6650 -p 8080:8080 apachepulsar/pulsar:2.11.0 bin/pulsar standalone
```

## Instalação

1. Clone o repositório:
   ```bash
   git clone https://github.com/seurepo/monitoramento-sinais-pulsar.git
   cd monitoramento-sinais-pulsar
   ```

2. Baixe a biblioteca do cliente Pulsar:
   ```bash
   wget https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client/2.11.0/pulsar-client-2.11.0.jar
   ```

## Uso

### Iniciando o Servidor
```bash
java -cp ".:pulsar-client-2.11.0.jar" HealthMonitoringServer
```

### Iniciando um Cliente
```bash
java -cp ".:pulsar-client-2.11.0.jar" HealthMonitoringClient
```

### Comandos do Sistema
Quando alertas forem exibidos, use os comandos:

| Comando            | Descrição                     |
|--------------------|-------------------------------|
| `CONFIRMAR <id>`   | Confirma um alerta             |
| `IGNORAR <id>`     | Ignora um alerta               |
| `CONTINUAR`        | Retoma o processamento         |
| `SAIR`             | Encerra o servidor             |

## Arquivos de Dados

| Arquivo                    | Descrição                       | Localização     |
|----------------------------|----------------------------------|----------------|
| `historico_pacientes.csv` | Todos os registros de sinais     | Raiz do projeto|
| `alertas_clinicos.log`    | Rastro de auditoria dos alertas  | Raiz do projeto|

Exemplo de formato CSV:
```
Data,Hora,Paciente,ID,FC,FR,Temp,Pressao_Sist,Pressao_Diast
30/03/2025,14:25:03,Felipe,b74aa81d,98,19,36.2,100,61
```

## Guia de Desenvolvimento

### Componentes Principais

1. **Servidor** (`HealthMonitoringServer.java`)
   - Consumidor de mensagens
   - Processador de alertas
   - Interpretador de comandos

2. **Cliente** (`HealthMonitoringClient.java`)
   - Simulador de sinais vitais
   - Produtor de dados

3. **Modelo de Dados** (`VitalSigns.java`)
   - ID e nome do paciente
   - Medidas vitais
   - Timestamps

### Compilação e Testes

Compile todos os arquivos:
```bash
javac -cp "pulsar-client-2.11.0.jar" *.java
```

Execute os testes:
```bash
# Inicie o servidor em um terminal
# Execute vários clientes em terminais separados
```

---

*Projeto desenvolvido por Felipe Leme Dias para a disciplina de Sistemas Distribuídos - Universidade Federal de Uberlândia*

