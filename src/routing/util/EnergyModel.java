/*
 * Copyright 2011 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing.util;

import java.util.Random;

import core.*;

/**
 * Energy model for routing modules. Handles power use from scanning (device
 * discovery), scan responses, and data transmission. If scanning is done more
 * often than 1/s, constant scanning is assumed (and power consumption does not
 * increase from {@link #scanEnergy} value).
 */

/* Oq devemos fazer:
 * 		Recarga de energia do dispositivo a cada X segundos;
 * 		Definição de energia inicial do dispositivo; (já vem feito)
 * 		Atualização da energia gasta com o escaneamento da rede; (ok)
 * 		Atualização da energia gasta com operação de envio e recebimento de mensagens. (ok)
 * 
 * 		DESENVOLVER
 * 			carregamento
 * 
 * o modelo de energia é uma expansão, ele não está "dentro" do nó		
 * usar o combus
 * 		a inicialização das variaveis se dá por meio do group
 * 		dtnhost para comunicar de fora
 * 		pôr um if no modulevaluechanged, o combus fica chamando ele
 * */
public class EnergyModel implements ModuleCommunicationListener {
	/** Initial units of energy -setting id ({@value}). Can be either a
	 * single value, or a range of two values. In the latter case, the used
	 * value is a uniformly distributed random value between the two values. */
	public static final String INIT_ENERGY_S = "initialEnergy";

	/** Energy usage per scanning (device discovery) -setting id ({@value}). */
	public static final String SCAN_ENERGY_S = "scanEnergy";

	/** Energy usage per scanning (device discovery) response -setting id
	 * ({@value}). */
	public static final String SCAN_RSP_ENERGY_S = "scanResponseEnergy";

	/** Energy usage per second when transferring data 
	 * -setting id ({@value}). */
	public static final String TRANSMIT_ENERGY_S = "transmitEnergy";

	/** Energy update warmup period -setting id ({@value}). Defines the
	 * simulation time after which the energy level starts to decrease due to
	 * scanning, transmissions, etc. Default value = 0. If value of "-1" is
	 * defined, uses the value from the report warmup setting
	 * {@link report.Report#WARMUP_S} from the namespace
	 * {@value report.Report#REPORT_NS}. */
	public static final String WARMUP_S = "energyWarmup";
	
	/** {@link ModuleCommunicationBus} identifier for the "current amount of
	 * energy left" variable. Value type: double */
	public static final String ENERGY_VALUE_ID = "Energy.value";
	
	//MINHAS ALTERAÇÕES
	/** É o tempo de simulação necessário para o dispositivo carregar completamente
	 * -setting id({@value})  */
	//public static final String RECHARGE_T = "rechargeTime";
	
	/** É o período com o qual o usuario coloca seu dispositivo para carregar */
	public static final String INTERVALO_CARGA = "rechargeInterval";
	
	/** É o nivel de bateria em que o usuário para de participar da comunicação	 
	 * -setting id({@value}) */
	public static final String NIVEL_CRITICO = "nivelCritico";
	
	/** é o consumo base do dispositivo do usuário -setting id({@value}) */
	public static final String CONSUMO = "consumo";
	//TERMINA AQUI

	/** Initial energy levels from the settings */
	private final double[] initEnergy;
	private double warmupTime;
	
	/** current energy level */
	private double currentEnergy;
	
	/** energy usage per scan */
	private double scanEnergy;
	
	/** energy usage per transmitted byte */
	private double transmitEnergy;
	
	/** energy usage per device discovery response */
	private double scanResponseEnergy;
	
	/** sim time of the last energy updated */
	private double lastUpdate;
	
	private ModuleCommunicationBus comBus;
	private static Random rng = null;
	
	//MINHAS ALTERAÇÕES
	/** tempo necessário para a carga total do dispositivo	0~100% */
	//private Double rechargeTime;
	
	/** quantidade de bateria por unidade de tempo (i.e) 1% de bateria a cada 10 seg*/
	//private Double rechargeRatio; //quantidade de energia que o dispositivo ganha por seg
	
	/** flag para indicar que o dispositivo está funcionando plugado na tomada */
	//private boolean isCarregando;
	
	/** marca o tempo de simulação em que o dispositivo atingiu zero de bateria */
	private Double timeEmpty;
	
	/** marca o nivel de bateria que o usuário considera critico, ou seja, o nivel em que a bateria deve
	 * ser salva, caso esteja abaixo o usuario não troca mensagens */
	private Double nivelCritico;
	
	/** marca o consumo de bateria do disposito por unidade de tempo, ex: um dispositivo x consome 3% 
	 * por hora*/
	private Double consumo;
	
	/** marca o tempo em que a bateria do dispositivo foi checada pela ultima vez
	 */
	private Double checaBateria;
	
	/** de quanto em quanto tempo o usuario da uma carga total*/
	private Double intevaloCarga;
	
	/** momento em que a bateria foi carregada pela ultima vez*/
	private Double cargaBateria;
	
	private boolean isCritico;
	// FIM ALTERAÇÕES

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public EnergyModel(Settings s) {
		this.initEnergy = s.getCsvDoubles(INIT_ENERGY_S);

		if (this.initEnergy.length != 1 && this.initEnergy.length != 2) {
			throw new SettingsError(INIT_ENERGY_S + " setting must have " +
					"either a single value or two comma separated values");
		}

		// é a quantidade de bateria que essas atividades consumem do dispositivo
		this.scanEnergy = s.getDouble(SCAN_ENERGY_S);
		this.transmitEnergy = s.getDouble(TRANSMIT_ENERGY_S);
		this.scanResponseEnergy = s.getDouble(SCAN_RSP_ENERGY_S);
		
		//ALTERAÇÕES
		//this.rechargeTime = s.getDouble(RECHARGE_T);
		this.intevaloCarga = s.getDouble(INTERVALO_CARGA);
		this.nivelCritico = s.getDouble(NIVEL_CRITICO);
		this.consumo = s.getDouble(CONSUMO);
		//FIM ALTERAÇÕES

		//é o tempo de simulação em que a bateria pode começar a comer
		if (s.contains(WARMUP_S)) {
			this.warmupTime = s.getInt(WARMUP_S);
			if (this.warmupTime == -1) {
				this.warmupTime = new Settings(report.Report.REPORT_NS).
					getInt(report.Report.WARMUP_S);
			}
		}
		else {
			this.warmupTime = 0;
		}
	}

	/**
	 * Copy constructor.
	 * @param proto The model prototype where setting values are copied from
	 */
	protected EnergyModel(EnergyModel proto) {
		this.initEnergy = proto.initEnergy;
		setEnergy(this.initEnergy);
		this.scanEnergy = proto.scanEnergy;
		this.transmitEnergy = proto.transmitEnergy;
		this.warmupTime  = proto.warmupTime;
		this.scanResponseEnergy = proto.scanResponseEnergy;
		this.comBus = null;
		this.lastUpdate = 0;
		this.cargaBateria = SimClock.getTime();
		
		
		//ALTEREI
		this.nivelCritico = proto.nivelCritico;
		this.consumo = proto.consumo;
		this.intevaloCarga = proto.intevaloCarga;
		this.checaBateria = SimClock.getTime();
		//this.rechargeRatio = this.rechargeTime/100.00;
		
		/*
		if(this.getEnergy() <= this.nivelCritico) {
			this.isCritico = true;
		}else {
			this.isCritico = false;
		}
		
		
		if(this.getEnergy() == 0.0) {
			this.isCarregando = true;
			this.timeEmpty = SimClock.getTime();
		}else {
			this.isCarregando = false;
			this.timeEmpty = null;
		}
		*/	
	}//fim proto

	public EnergyModel replicate() {
		return new EnergyModel(this);
	}

	/**
	 * Sets the current energy level into the given range using uniform
	 * random distribution.
	 * @param range The min and max values of the range, or if only one value
	 * is given, that is used as the energy level
	 */
	protected void setEnergy(double range[]) {
		if (range.length == 1) { //foi dado um valor direto para ser a qntd inicial de bateria
			double numAleatorio = Math.random() * 50;
			//System.out.printf("%f\n", numAleatorio);
			this.currentEnergy = range[0] - numAleatorio;
			System.out.printf("%f\n", this.currentEnergy);
		}
		else {
			if (rng == null) {
				rng = new Random((int)(range[0] + range[1]));
				System.out.printf("%d\n", rng);
			}
			this.currentEnergy = range[0] +
				rng.nextDouble() * (range[1] - range[0]);
				System.out.printf("%.2lf\n", rng);
		}
	}
	
	/** simula o consumo da bateria tendo em vista seu consumo natural com base em outros aplicativos
	 *  ou do proprio sistema, toda vez que alguma função for checar o estado da bateria vamos 
	 *  levar em conta o passar do tempo e consequentemente seu consumo
	 * */	
	public Double simulaConsumo() {
		//tempo decorrigo desde a uma checagem na bateria
		Double tempoDecorrido = Math.abs(this.checaBateria - SimClock.getTime());
		if (tempoDecorrido <= 0.0)
			tempoDecorrido = 0.0;
		//marca o quanto consumiu no tempo que decorreu
		
		int centena = (int) (tempoDecorrido%100);
		int dezena = (int) (tempoDecorrido%10);
		int unidade =(int) (tempoDecorrido%1);
		tempoDecorrido = (double) (centena + dezena + unidade);
		
		Double bateriaAtual =  tempoDecorrido * this.consumo;
		
		//System.out.printf("tempo decorrido: %f\n", tempoDecorrido);
		//System.out.printf("Consumo: %f\n", bateriaAtual);
		
		this.checaBateria = SimClock.getTime();
		return bateriaAtual;
		//a bateria zerou, logo a recarga inicia
		
		//return aux;
		/*
		if (bateriaAtual <= 0.0) {
			this.currentEnergy = 0.0;
			this.isCritico = true;
			this.timeEmpty = SimClock.getTime();
		}else {
			this.currentEnergy = bateriaAtual;
			if (bateriaAtual <= this.nivelCritico) {
				this.isCritico = true;
			}else {
				this.isCritico = false;
			}
		}	
		*/
	}
	
	
	/** Seta a carga do nó para 100% tendo em vista o tempo em que ele foi carregado pela ultima vez
	 * e o intervalo de tempo com que os usuarios carregam seus dispositivos
	 */
	public void carregamento() {
		Double tempoAtual = SimClock.getTime();		
		//qr dizer que o usuário já passou do tempo de carregar sua bateria
		this.cargaBateria = tempoAtual;
		this.isCritico = false;
		//this.currentEnergy = 100.0;
		//comBus.updateProperty(ENERGY_VALUE_ID, 100.0);
	}
	
	/**
	 * Returns the current energy level
	 * @return the current energy level
	 */
	public double getEnergy() {
		//simulaConsumo();
		return this.currentEnergy;
	}

	/**
	 * Updates the current energy so that the given amount is reduced from it.
	 * If the energy level goes below zero, sets the level to zero.
	 * Does nothing if the warmup time has not passed.
	 * @param amount The amount of energy to reduce
	 */
	protected void reduceEnergy(double amount) {
		if (SimClock.getTime() < this.warmupTime) {
			return;
		}

		if (comBus == null) {
			return; /* model not initialized (via update) yet */
		}
		
		if(this.currentEnergy == 0.0) {
			return;
		}
		
		if (amount >= this.currentEnergy) {
			comBus.updateProperty(ENERGY_VALUE_ID, 0.0);	
			this.currentEnergy = 0.0;
			this.isCritico = true;
		} else {			
			comBus.updateDouble(ENERGY_VALUE_ID, -amount);
			this.currentEnergy = this.currentEnergy - amount;
		}
	}

	/**
	 * Reduces the energy reserve for the amount that is used when another
	 * host connects (does device discovery)
	 */
	public void reduceDiscoveryEnergy() {
		//simulaConsumo();
		reduceEnergy(this.scanResponseEnergy);
	}

	/**
	 * Reduces the energy reserve for the amount that is used by sending data
	 * and scanning for the other nodes.
	 */
	public void update(NetworkInterface iface, ModuleCommunicationBus comBus) {
		//simulaConsumo();
		double simTime = SimClock.getTime();
		double delta = simTime - this.lastUpdate;
		
		//WTF is this? watch ur prafanity!
		if (this.comBus == null) {
			this.comBus = comBus;
			this.comBus.addProperty(ENERGY_VALUE_ID, this.currentEnergy);
			this.comBus.subscribe(ENERGY_VALUE_ID, this);
		}
		/*
		 //o dispositivo está em um nivel critico, portanto não participará da comunicação
		if(this.isCritico) {
			this.lastUpdate = simTime;
			return;
		}
		 */
		
		if (simTime > this.lastUpdate && iface.isTransferring()) {
			/* sending or receiving data */
			reduceEnergy(delta * this.transmitEnergy);
		}
		this.lastUpdate = simTime;

		if (iface.isScanning()) {
			/* scanning at this update round */
			if (iface.getTransmitRange() > 0) {
				if (delta < 1) {
					reduceEnergy(this.scanEnergy * delta);
				} else {
					reduceEnergy(this.scanEnergy);
				}
			}
		}
	}

	/**
	 * Called by the combus if the energy value is changed
	 * @param key The energy ID
	 * @param newValue The new energy value
	 */
	
	//colocar pro cara carregar o celular de x em x tempo ou caso ele atinja nivel critico
	public void moduleValueChanged(String key, Object newValue) {
		Double novo = (Double)newValue;
		
		Double consumiu = this.simulaConsumo();
		//System.out.printf("nivel novo: %f\n", novo);
		//System.out.printf("bateria consumida passivamente: %f\n", consumiu);
		
		Double nivelAtual = novo - consumiu;
		//System.out.printf("%.2f\n", nivelAtual);
		//System.out.printf("Energia Atual: %.2f\n", this.currentEnergy);
		//System.out.printf("teste: %s\n", Energy.value);
		
		Double tempoUltimaCarga = SimClock.getTime() - this.cargaBateria;
		
		if(tempoUltimaCarga >= 1000.0) {
			tempoUltimaCarga = tempoUltimaCarga - 1000.0;
		}
		//System.out.printf("Tempo Ultima Carga:%f\n", tempoUltimaCarga);
		System.out.printf("%f\n", this.currentEnergy);
		/*
		if(nivelAtual <= 0.0) {
			System.out.println("o nó zerou a bateria");
			//this.currentEnergy = 0.0; //evitar que a bateria negative
			this.isCritico = true;
			this.carregamento();
			this.currentEnergy = 100.0;
			//this.comBus.subscribe(ENERGY_VALUE_ID, this); //informando o id e o obj
			//comBus.updateProperty(ENERGY_VALUE_ID, 100.0);
			//this.carregamento();
			System.out.printf("Carregando Bateria\n");
		}else */
			
			if(nivelAtual <= this.nivelCritico || tempoUltimaCarga >= this.intevaloCarga) { // teste
			//this.currentEnergy = 100.0;
			//comBus.updateProperty(ENERGY_VALUE_ID, 100.0);
			this.isCritico = true;
			this.carregamento();
			//this.currentEnergy = 100.0;
			comBus.updateProperty(ENERGY_VALUE_ID, 100.0);
			System.out.println("Carregando Bateria");
			//isCritico = true;
		}else {
			this.currentEnergy = nivelAtual;
			//comBus.updateProperty(ENERGY_VALUE_ID, 100.0);
			//comBus.updateProperty(ENERGY_VALUE_ID, nivelAtual);
		}
	}

}
