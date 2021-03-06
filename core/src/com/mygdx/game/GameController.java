package com.mygdx.game;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.mygdx.engine.Camera;
import com.mygdx.engine.Engine;
import com.mygdx.engine.GameObject;
import com.mygdx.engine.Renderer;
import com.mygdx.engine.Scene;
import com.mygdx.engine.Script;
import com.mygdx.engine.Transform;
import com.mygdx.engine.UI;
import com.mygdx.game.units.FlockingGroup;
import com.mygdx.game.units.FormationGroup;
import com.mygdx.game.units.HeavyUnit;
import com.mygdx.game.units.LightUnit;
import com.mygdx.game.units.PhantomUnit;
import com.mygdx.game.units.SpawnBaseUnit;
import com.mygdx.game.units.TestUnit;
import com.mygdx.game.units.Unit;
import com.mygdx.game.units.Unit.Mode;
import com.mygdx.game.units.UnitGroup;
import com.mygdx.ia.Bot;
import com.mygdx.ia.BotScript;
import com.mygdx.ia.Heuristic;
import com.mygdx.ia.Path;
import com.mygdx.ia.Pathfinding;
import com.mygdx.ia.PathfindingConfig;
import com.mygdx.ia.behaviours.basic.AlignUA;
import com.mygdx.ia.behaviours.basic.AntiAlignUA;
import com.mygdx.ia.behaviours.basic.ArriveU;
import com.mygdx.ia.behaviours.basic.ArriveUA;
import com.mygdx.ia.behaviours.basic.FleeU;
import com.mygdx.ia.behaviours.basic.FleeUA;
import com.mygdx.ia.behaviours.basic.SeekU;
import com.mygdx.ia.behaviours.basic.SeekUA;
import com.mygdx.ia.behaviours.basic.WanderU;
import com.mygdx.ia.behaviours.delgate.Evade;
import com.mygdx.ia.behaviours.delgate.Face;
import com.mygdx.ia.behaviours.delgate.LookWhereYouGoing;
import com.mygdx.ia.behaviours.delgate.ObstacleAvoidance;
import com.mygdx.ia.behaviours.delgate.PathFollowing;
import com.mygdx.ia.behaviours.delgate.Persue;
import com.mygdx.ia.behaviours.delgate.Wander;
import com.mygdx.ia.behaviours.group.Attraction;
import com.mygdx.ia.behaviours.group.Cohesion;
import com.mygdx.ia.behaviours.group.Separation;

/**
 * Script principal que llevará toda la lógica del juego.
 */
public class GameController extends Script {
	
	// variables privadas para el funcionamiento del juego.
	
	private GameObject selected;
	private List<Unit> selectedGroup;
	private PathfindingConfig config;
	private Scene scene;
	private UI ui;
	private List<UnitGroup> groups;
	
	private SpawnBaseUnit base0,base1;
	
	private float top;
	private float deltaCam;
	private float deltaZoom;
	private boolean showInfo;
	int behaviour = 0;
	private int winner;
	
	
	@Override
	public void start() {
		
		winner = -1;
		showInfo = false;
		deltaCam = 4;
		deltaZoom = 0.1f;
		top = Gdx.graphics.getHeight()-10;
		
		// OBTENEMOS LA ESCENA ACTUAL PARA PODER BUSCAR GAME OBJECTS (usar la funcion find)
		scene = Engine.getInstance().getCurrentScene(); 
		
		// BASES
		base0 = (SpawnBaseUnit) scene.find("base0");
		base1 = (SpawnBaseUnit) scene.find("base1");
						
		// COGEMOS EL ui PARA NO TENER QUE COGERLO CADA UPDATE
		ui = UI.getInstance();
		
		selectedGroup = new LinkedList<Unit>();
		groups = new LinkedList<UnitGroup>();
		
		
		// CONFIGURAMOS EL PATHFINDING
		
		/*
		 * Para cada tipo de unidad se va a aplicar una heurística diferente
		 */
		config = new PathfindingConfig();
		config.bindHeuristic(LightUnit.class, Heuristic.EUCLIDEAN);
		config.bindHeuristic(PhantomUnit.class, Heuristic.MANHATTAN); 
		config.bindHeuristic(HeavyUnit.class, Heuristic.CHEBYCHEV); 
		config.bindHeuristic(TestUnit.class, Heuristic.EUCLIDEAN);
		Pathfinding.setConfig(config);
		
		/*
		 * WAYPOINTS
		 */

		Waypoints.getInstance().loadWaypoints(scene);
	}
	
	@Override
	public void update() {
		
		/*
		 * Cada update comprueba las condiciones de victoria.
		 */
		if(Engine.getInstance().getCurrentScene().getSceneName().equals("game"))
			checkVictory();
		
	
		/*
		 * Procesa el input
		 */
		if(Gdx.input.isKeyJustPressed(Input.Keys.D)){
			if(!Engine.getInstance().getCurrentScene().getSceneName().equals("test"))
					debugOnOff();
		}
		else if(Gdx.input.isKeyPressed(Input.Keys.D)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
				Vector2 pos = player.getComponent(Transform.class).position.cpy();
				pos.x+=1;
				player.getComponent(Transform.class).position=pos;
			} 
		}
		else if(Gdx.input.isKeyPressed(Input.Keys.A)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
				Vector2 pos = player.getComponent(Transform.class).position.cpy();
				pos.x-=1;
				player.getComponent(Transform.class).position=pos;
			} 
		}
		else if(Gdx.input.isKeyPressed(Input.Keys.S)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
				Vector2 pos = player.getComponent(Transform.class).position.cpy();
				pos.y-=1;
				player.getComponent(Transform.class).position=pos;
			} 
		}
		else if(Gdx.input.isKeyPressed(Input.Keys.Q)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
				Float ori = player.getComponent(Transform.class).orientation;
				ori+=1;
				player.getComponent(Transform.class).orientation=ori;
			} 
		}
		else if(Gdx.input.isKeyPressed(Input.Keys.E)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
				Float ori = player.getComponent(Transform.class).orientation;
				ori-=1;
				player.getComponent(Transform.class).orientation=ori;
			} 
		}
		else if(Gdx.input.isKeyPressed(Input.Keys.W)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
				Vector2 pos = player.getComponent(Transform.class).position.cpy();
				pos.y+=1;
				player.getComponent(Transform.class).position=pos;
			} 
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.I)){
			showInfo = ! showInfo;
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
			clearSelected();
		else if(Gdx.input.isKeyJustPressed(Input.Keys.G))
			formationGroup();
		else if(Gdx.input.isKeyJustPressed(Input.Keys.F))
			flockingGroup();
		else if(Gdx.input.isKeyJustPressed(Input.Keys.S))
			splitGroup();
		else if(Gdx.input.isKeyJustPressed(Input.Keys.TAB))
			tab();
		else if(Gdx.input.isButtonPressed(Input.Buttons.LEFT))
			leftClick();
		else if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
			rightClick();
		else if(Gdx.input.isKeyJustPressed(Input.Keys.PLUS))
			zoom(-deltaZoom);
		else if(Gdx.input.isKeyJustPressed(Input.Keys.MINUS))
			zoom(deltaZoom);
		else if(Gdx.input.isKeyJustPressed(Input.Keys.UP))
			moveCamera(0,deltaCam);
		else if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN))
			moveCamera(0,-deltaCam);
		else if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT))
			moveCamera(-deltaCam,0);
		else if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))
			moveCamera(deltaCam,0);
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 0){
					behaviour = 1;
				} else if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new AlignUA(bot.getComponent(BotScript.class), player.getComponent(BotScript.class), 30f, 20f, 5f));
				}else if(behaviour == 2){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					bot.getComponent(BotScript.class).addBehaviour(new Wander(bot.getComponent(BotScript.class), 15, 5, 5f, 2, 2, 1, 2));	
				
					}else{//behaviour == 3
					Bot bot = (Bot) Engine.getInstance().getCurrentScene().find("testUnit");
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot bot2 = (Bot) Engine.getInstance().getCurrentScene().find("group1");
					bot2.getComponent(BotScript.class).clearAllBehaviours();
					Bot bot3 = (Bot) Engine.getInstance().getCurrentScene().find("group2");
					bot3.getComponent(BotScript.class).clearAllBehaviours();
					LinkedList<BotScript> lista = new LinkedList<BotScript>();
					lista.add(bot.getComponent(BotScript.class));
					lista.add(bot2.getComponent(BotScript.class));
					lista.add(bot3.getComponent(BotScript.class));
					bot.getComponent(BotScript.class).addBehaviour(new Attraction(bot.getComponent(BotScript.class), lista, 10*Scene.SCALE,2*Scene.SCALE));
					bot2.getComponent(BotScript.class).addBehaviour(new Attraction(bot2.getComponent(BotScript.class), lista, 10*Scene.SCALE,2*Scene.SCALE));
					bot3.getComponent(BotScript.class).addBehaviour(new Attraction(bot3.getComponent(BotScript.class), lista, 10*Scene.SCALE,2*Scene.SCALE));
				}
			}else {
			offensive();
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 0){
					behaviour = 2;
				} else if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new AntiAlignUA(bot.getComponent(BotScript.class), player.getComponent(BotScript.class), 30f, 20f, 5f));
				}else if(behaviour == 2){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new Face(bot.getComponent(BotScript.class), player.getComponent(BotScript.class), 15, 5, 3f));
				}else{//behaviour == 3
					Bot bot = (Bot) Engine.getInstance().getCurrentScene().find("testUnit");
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot bot2 = (Bot) Engine.getInstance().getCurrentScene().find("group1");
					bot2.getComponent(BotScript.class).clearAllBehaviours();
					Bot bot3 = (Bot) Engine.getInstance().getCurrentScene().find("group2");
					bot3.getComponent(BotScript.class).clearAllBehaviours();
					LinkedList<BotScript> lista = new LinkedList<BotScript>();
					lista.add(bot.getComponent(BotScript.class));
					lista.add(bot2.getComponent(BotScript.class));
					lista.add(bot3.getComponent(BotScript.class));
					bot.getComponent(BotScript.class).addBehaviour(new Cohesion(bot.getComponent(BotScript.class), lista,10*Scene.SCALE));
					bot2.getComponent(BotScript.class).addBehaviour(new Cohesion(bot2.getComponent(BotScript.class), lista,10*Scene.SCALE));
					bot3.getComponent(BotScript.class).addBehaviour(new Cohesion(bot3.getComponent(BotScript.class), lista,10*Scene.SCALE));
				
					}
			}else {
			defensive();
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 0){
					behaviour = 3;
				} else if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new ArriveU(bot.getComponent(BotScript.class),player.getComponent(BotScript.class),1*Scene.SCALE,5f));
				}else if(behaviour == 2){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new Evade(bot.getComponent(BotScript.class),player.getComponent(BotScript.class), 5));
				
				}else{//behaviour == 3
					Bot bot = (Bot) Engine.getInstance().getCurrentScene().find("testUnit");
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot bot2 = (Bot) Engine.getInstance().getCurrentScene().find("group1");
					bot2.getComponent(BotScript.class).clearAllBehaviours();
					Bot bot3 = (Bot) Engine.getInstance().getCurrentScene().find("group2");
					bot3.getComponent(BotScript.class).clearAllBehaviours();
					LinkedList<BotScript> lista = new LinkedList<BotScript>();
					lista.add(bot.getComponent(BotScript.class));
					lista.add(bot2.getComponent(BotScript.class));
					lista.add(bot3.getComponent(BotScript.class));
					bot.getComponent(BotScript.class).addBehaviour(new Separation(bot.getComponent(BotScript.class), lista,10*Scene.SCALE,1000*Scene.SCALE));
					bot2.getComponent(BotScript.class).addBehaviour(new Separation(bot2.getComponent(BotScript.class), lista,10*Scene.SCALE,1000*Scene.SCALE));
					bot3.getComponent(BotScript.class).addBehaviour(new Separation(bot3.getComponent(BotScript.class), lista,10*Scene.SCALE,1000*Scene.SCALE));
				}
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new ArriveUA(bot.getComponent(BotScript.class),player.getComponent(BotScript.class),1*Scene.SCALE,3*Scene.SCALE,5f));
				}else if(behaviour == 2){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new LookWhereYouGoing(bot.getComponent(BotScript.class), player.getComponent(BotScript.class), 15, 5, 3f));
					
				}
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new FleeU(bot.getComponent(BotScript.class),player.getComponent(BotScript.class)));
				}else if(behaviour == 2){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					bot.getComponent(BotScript.class).addBehaviour(new WanderU(bot.getComponent(BotScript.class)));
					bot.getComponent(BotScript.class).addBehaviour(new ObstacleAvoidance(bot.getComponent(BotScript.class), 2*Scene.SCALE, 1*Scene.SCALE, Engine.getInstance().getCurrentScene()) );
				}
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new FleeUA(bot.getComponent(BotScript.class),player.getComponent(BotScript.class)));
				}else if(behaviour == 2){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					Path path = new Path(0.5f*Scene.SCALE);
					Pathfinding pathfinding = new Pathfinding(Engine.getInstance().getCurrentScene(), bot, bot.getComponent(BotScript.class).getPosition() ,player.getComponent(BotScript.class).getPosition(), Config.getInstacia().getUnitMap(LightUnit.class));
					path.setParams(pathfinding.generate());
					path.setTeam(0);
					if( ! path.isEmpty())
						bot.getComponent(BotScript.class).addBehaviour(new PathFollowing(bot.getComponent(BotScript.class), path, 0f));
					
				}
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new SeekU(bot.getComponent(BotScript.class),player.getComponent(BotScript.class)));
				}else if(behaviour == 2){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new Persue(bot.getComponent(BotScript.class), player.getComponent(BotScript.class), 5f));
					
				}
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					Bot player = (Bot) Engine.getInstance().getCurrentScene().find("testPlayer");
					bot.getComponent(BotScript.class).addBehaviour(new SeekUA(bot.getComponent(BotScript.class),player.getComponent(BotScript.class)));
				}
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)){
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 1){
					Bot bot = (Bot)selected;
					bot.getComponent(BotScript.class).clearAllBehaviours();
					bot.getComponent(BotScript.class).addBehaviour(new WanderU(bot.getComponent(BotScript.class)));
				}
			}
		}
		else if(Gdx.input.isKeyJustPressed(Input.Keys.T))
			totalWar();
		else if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
			if(Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
				if(behaviour == 3){
					behaviour = 0;
				} else if(behaviour == 1){
					behaviour = 0;
				}else if(behaviour == 2){
					behaviour = 0;
				}else{
					showInfo = false;
					testMode();
				}
			} else {
			showInfo = false;
			testMode();
			}
		}
		
		//procesar según test mode o no
		
		drawInfo();
	}
	
	
	/**
	 * Inicia el modo test para probar los steerings, etc.
	 */
	private void testMode() {
		if(!Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
			Engine.getInstance().changeScene("test");
			behaviour=0;
			Engine.getInstance().start();
		} else {
			Engine.getInstance().changeScene("game");
			Engine.getInstance().start();
		}
	}

	/**
	 * Comprueba si se ha llegado a condiciones de victoria.
	 */
	private void checkVictory(){
		
		
		
		if(base0.getLife() == 0){
			
			winner = 1;
			for (GameObject obj : scene.getGameObjectsWithClass(Unit.class)) {
				scene.removeObject(obj);
			}
			
		}else if(base1.getLife() == 0){
			
			winner = 0;
			for (GameObject obj : scene.getGameObjectsWithClass(Unit.class)) {
				scene.removeObject(obj);
			}
			
		}
		
		if(winner > -1){
			
			
			ui.drawText("TEAM " + winner + " WINS", 100,200, Color.WHITE);
		}
	}
	
	/**
	 * Devuelve la posición del cursor.
	 * @return
	 */
	private Vector2 getCursorPosition(){
		// OBTENEMOS LA COORDENADA EN EL MUNDO DEL CURSOR
		final Vector3 mouse_position = new Vector3(0,0,0);
		mouse_position.set(Gdx.input.getX(), Gdx.input.getY(), 0);
		final Vector3 mouse_world = scene.find("cam").getComponent(Camera.class).camera.unproject(mouse_position);
		return new Vector2(mouse_world.x,mouse_world.y);
	}
	
	/**
	 * Mueve la camara al objetivo.
	 */
	private void lookAt(GameObject obj){
		scene.find("cam").getComponent(Camera.class).setTarget(obj);
	}
	
	/**
	 * Lanza un raycast y si encuentra algo, lo almacena en la variable "selected".
	 */
	private void raycastSelect(){
		
		Vector2 cursor = getCursorPosition();
		
		Vector2 camPos = scene.find("cam").getComponent(Transform.class).position;
	
//		GameObject target = scene.find("cam").getComponent(Camera.class).getTarget();
//		if(target != null)
//			selected = target;
		
		// SE LANZA UN RAYCAST
		scene.getWorld().rayCast(new RayCastCallback() {
			
			@Override
			public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
				
				// SI HEMOS CLICADO DENTRO DEL PERSONAJE
				if(fixture.testPoint(getCursorPosition())){
					
					// TODOS LOS COLLIDERS LLEVAN ASOCIADO UN USER DATA. HEMOS PUESTO EL GAME OBJECT COMO USER DATA.
					// ASI LO PODEMOS RECUPERAR CADA VEZ QUE COLISIONA EL RAY CAST.
					if(fixture.getUserData() != null)
						selected = (GameObject) fixture.getUserData();
					
//					System.out.println("Click! ");
				}
				
				return 1;
			}
		}, camPos, cursor);
	}
	
	/**
	 * Añade al objeto al grupo de seleccionados
	 * @param o
	 */
	private void addToSelectedGroup(GameObject o){
		if(!selectedGroup.contains(o))
			selectedGroup.add((Unit) o);
	}
	
	/**
	 * Dibuja el circulo de seleccion sobre el objeto
	 * @param o
	 */
	private void drawSelectionCircle(GameObject o){
		
		Unit unit = (Unit) o;
				
		if(unit.getGroup() != null){
			for (Unit u : unit.getGroup().getUnits()) {
				ui.drawCircleWorld(u.getComponent(Transform.class).position, o.getComponent(Renderer.class).getSprite().getWidth(), Color.YELLOW, false);
			}
		}else{
			ui.drawCircleWorld(o.getComponent(Transform.class).position, o.getComponent(Renderer.class).getSprite().getWidth(), Color.YELLOW, false);
		}
	}
	
	/**
	 * Dibuja el circulo de elemento seleccionado actual
	 * @param o
	 */
	private void drawActiveCircle(GameObject o){
		
		Unit unit = (Unit) o;
		
		float w = o.getComponent(Renderer.class).getSprite().getWidth();
		
		Vector2 pos = o.getComponent(Transform.class).position;
		
		ui.drawCircleWorld(pos, w-(w*0.1f), Color.YELLOW, false);
		ui.drawCircleWorld(pos, unit.getRangeVision(), Color.GREEN, false);
		ui.drawCircleWorld(pos, unit.getRangeAttack(), Color.RED, false);
				
		if(unit.getGroup() != null){
			for (Unit u : unit.getGroup().getUnits()) {
				ui.drawCircleWorld(u.getComponent(Transform.class).position, w, Color.YELLOW, false);
			}
		}
	}
	
	/**
	 * Activa / Desactiva la interfaz
	 */
	private void debugOnOff(){
		if(ui.isEnabled())
			ui.disable();
		else
			ui.enable();
	}
	
	/**
	 * Aplica zoom
	 * @param zoom
	 */
	private void zoom(float zoom){
		scene.find("cam").getComponent(Camera.class).addZoom(zoom);
	}
	
	/**
	 * Mueve la camara al punto x,y
	 * @param x
	 * @param y
	 */
	private void moveCamera(float x, float y){
		scene.find("cam").getComponent(Camera.class).setTarget(null);
		
		scene.find("cam").getComponent(Camera.class).setTarget(null);
		scene.find("cam").getComponent(Transform.class).position.add(new Vector2(x, y).scl(Scene.SCALE));
	}
	
	/**
	 * Mueve la camara a la posicion del cursor
	 */
	private void moveCameraToCursor(){
		scene.find("cam").getComponent(Camera.class).setTarget(null);
		
		Vector2 cursor = getCursorPosition();
		
		Vector2 pos = scene.find("cam").getComponent(Transform.class).position;
				
		Vector2 difference = cursor.cpy().sub(pos);
		float len = difference.len();
		
		pos.add(difference.nor().scl(len/(2*Scene.SCALE)));	
		
		ui.drawLineWorld(pos,cursor, Color.GRAY, false);
		
	}
	
	/**
	 * Limpia los seleccionados
	 */
	private void clearSelected(){
		selected = null;
		selectedGroup.clear();
	}
	
	/**
	 * Dibuja la informacion de debug del juego
	 */
	private void drawInfo(){
		
		ui.drawCircleWorld(getCursorPosition(), 0.1f*Scene.SCALE, Color.WHITE, false);

		if(selected != null){
			ui.drawText("> total selected: "+selectedGroup.size(), 10, 40, Color.WHITE);
			ui.drawText("> current selected: " + selected.getName(), 10, 20, Color.WHITE);
			drawActiveCircle(selected);
		}
		
		if( ! selectedGroup.isEmpty()){
			for (Unit unit : selectedGroup) {
				drawSelectionCircle(unit);
			}
		}
		
		float lineY = top;
		
		if(showInfo){
			ui.drawText("> Press 'I' to HIDE info", 10, lineY, Color.WHITE); lineY-=40;
			ui.drawText("> Press 'D' to enable/disable the DEBUG RENDER", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'Right Click' to select a DESTINY or ATTACK a target or PATROL a base", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'Left Click' to SELECT an unit", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'ESC' to DESELECT an unit", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'Drag Left Click' or 'Arroy Keys' to move the CAMERA", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'Ctrl + Left Click' to select MULTIPLE units", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'TAB' to switch to the next unit", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press '1' to set unit in OFFENSIVE MODE", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press '2' to set unit in DEFENSIVE MODE", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'T' to enable << TOTAL WAR >>", 10, lineY, Color.WHITE); lineY-=20;
			ui.drawText("> Press 'K' to enable << TEST MODE >>", 10, lineY, Color.WHITE); lineY-=20;
			
			lineY-=20;
					
			if(selectedGroup.size() > 1){
				ui.drawText("> Press 'F' to use FLOCKING in multiple units", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press 'G' to join in CIRCLE shape multiple units", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press 'S' to split multiple units", 10, lineY, Color.WHITE); lineY-=20;
				
			}
		}else if (Engine.getInstance().getCurrentScene().getSceneName().equals("test")){
			if(behaviour == 0){
				ui.drawText("> Press '1' to basic behaviours", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '2' to delgate behaviours", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '3' to group behaviours", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press 'K' to disable << TEST MODE >>", 10, lineY, Color.WHITE); lineY-=20;
			} else if(behaviour == 1) {
				ui.drawText("> Press '1' to Align behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '2' to AntiAlign behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '3' to Uniform Arrive behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '4' to Acelerated Arrive behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '5' to Uniform Flee behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '6' to Acelerated Flee behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '7' to Uniform Seek behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '8' to Acelerated Seek behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '9' to Uniform Wander behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press 'K' to return", 10, lineY, Color.WHITE); lineY-=20;
			}else if(behaviour == 2) {
				ui.drawText("> Press '1' to Wander behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '2' to Face behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '3' to Evade behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '4' to LookWhereYouGoing behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '5' to ObstacleAvoidance behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '6' to PathFollowing behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '7' to Persue behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press 'K' to return", 10, lineY, Color.WHITE); lineY-=20;
			}else{ // behaviour == 3
				ui.drawText("> Press '1' to Attraction behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '2' to Cohesion behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press '3' to Separation behaviour", 10, lineY, Color.WHITE); lineY-=20;
				ui.drawText("> Press 'K' to return", 10, lineY, Color.WHITE); lineY-=20;
			}
		}else{
			ui.drawText("> Press 'I' to SHOW info", 10, top, Color.WHITE);
		}
		
		

	}
	
	/**
	 * Funcion que se llama cuando se hace click derecho
	 */
	private void rightClick(){
		/*
		 * RIGHT CLICK sirve para:
		 * 1. atacar
		 * 2. ir hacia un punto
		 */
		
//		System.out.println(scene.getTile(scene.fromWorldtoGridCoordinates(getCursorPosition())).getId());
		
		if (selected != null){
			Unit unit = ((Unit)selected); // cogemos el seleccionado actual
			
			// obtenemos el nuevo
			selected = null;
			raycastSelect(); // seleccionamos uno nuevo y lo usamos como target
			
			Unit selectedUnit = (Unit) selected;
			
			if(selectedUnit != null && unit.getName() != selectedUnit.getName() && selectedUnit instanceof SpawnBaseUnit && ((SpawnBaseUnit)selectedUnit).getTeam() == unit.getTeam()){
			
				unit.setPatrolBase(true);
				
				
			// si hemos clickado a una unidad diferente a la nuestra, entonces la atacamos
			}else if(selectedUnit != null && unit.getName() != selectedUnit.getName()){
				
//				if(unit.getGroup() != null){
//					unit.getGroup().setTargetAttack((Unit) selected);
//				}else
					unit.setTargetAttack(selectedUnit);
								
			}else{
				// si no, lo que queremos es tomarlo como un punto de destino
				
//				if(unit.getGroup() != null){
//					unit.getGroup().setDestiny(getCursorPosition());
//				}else
					unit.setDestiny(getCursorPosition());
				
			}
			
			selected = unit; // restauramos el selected
			
//			selectedGroup.clear();
		}
	}
	
	private void select(Unit unit){
		if(unit != null){
						
			if(unit.getGroup() == null && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)){
				addToSelectedGroup(unit);
			}else{
				
				selectedGroup.clear();
				
				if(unit.getGroup() != null){
					for (Unit u : unit.getGroup().getUnits()) {
						addToSelectedGroup(u);
					}
				}else
					addToSelectedGroup(unit);
				
				selected = unit;
			}
		}
	}
	
	/**
	 * Funcion que se llama cuando se hace click izquierdo
	 */
	private void leftClick(){
		/*
		 * LEFT CLICK sirve para:
		 * 1. seleccionar a un personaje
		 * 2. left click + ctrl: agrupa unidades
		 * 3. mover la camara
		 */
		
//		System.out.println(scene.fromWorldtoGridCoordinates(getCursorPosition()));

		moveCameraToCursor();
		
		raycastSelect();
		
		select((Unit) selected);
	}
	
	/**
	 * Establece las unidades en modo ofensivo.
	 */
	private void offensive(){
//		if(selected != null){
//			((Unit) selected).setMode(Mode.OFFENSIVE);
//		}
		
		for (Unit u : selectedGroup) {
			u.setMode(Mode.OFFENSIVE);
		}
	}
	
	/**
	 * Establece las unidades en modo defensivo.
	 */
	private void defensive(){
//		if(selected != null){
//			((Unit) selected).setMode(Mode.DEFENSIVE);
//		}
		
		for (Unit u : selectedGroup) {
			u.setMode(Mode.DEFENSIVE);
		}
	}
	
	/**
	 * Funcion que se llama cuando se pulsa TAB
	 */
	private void tab(){
		List<Unit> units = scene.getGameObjectsWithClass(Unit.class);
		
		if(selected != null){
							
			int i = 0;
			for (Unit unit : units) {
				
				if(unit.getName() == selected.getName()){
					
					clearSelected();
					
					selected = units.get((i+1)%units.size());
					
					lookAt(selected);
					addToSelectedGroup(selected);
					
					break;
				}
				
				i++;
			}
		}else{
			
			clearSelected();
			
			selected = units.get(0);
			
			lookAt(selected);
			addToSelectedGroup(selected);
		}
	}


	/**
	 *  Join (Unir). En este modo podemos unir unidades en un mismo grupo para que adopten un 
	 * comportamiento de Flocking.
	 */
	private void flockingGroup(){
		
		if(selectedGroup.size() > 1){

			UnitGroup unitGroup = new FlockingGroup(null,selectedGroup);	
			unitGroup.join();
			
			groups.add(unitGroup);
			
			selectedGroup.clear();
		}
		
	}
	
	private void formationGroup(){
		
		if(selectedGroup.size() > 1){

			UnitGroup unitGroup = new FormationGroup(null,selectedGroup);	
			unitGroup.join();
			
			groups.add(unitGroup);
			Engine.getInstance().getCurrentScene().addObject(unitGroup);
			
			selectedGroup.clear();
		}
		
	}
	
	/**
	 *  Split (Separar). En este modo podemos separar unidades que antes estaban en el mismo
	 * grupo.
	 */
	private void splitGroup(){
		
		Unit unit = (Unit) selected;
		
		if(unit.getGroup() != null){
			unit.getGroup().split();
			groups.remove(unit.getGroup());
			Engine.getInstance().getCurrentScene().removeObject(unit.getGroup());
		}
	}
	
	/*
	 * Activa/Desactiva el modo Total War
	 */
	private void totalWar(){
		
		
		if(base0.isTotalWar())
			base0.disableTotalWar();
		else
			base0.enableTotalWar();
		
		if(base1.isTotalWar())
			base1.disableTotalWar();
		else
			base1.enableTotalWar();
	}
	
	
}