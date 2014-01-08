/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.alternativmud.system.unityserver;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import net.alternativmud.App;
import net.alternativmud.framework.Service;
import net.alternativmud.game3d.UnityScenes;
import net.alternativmud.logic.User;
import net.alternativmud.logic.world.characters.UCharacter;
import net.alternativmud.system.nebus.server.AuthenticatedBusSubscriber;
import net.alternativmud.system.nebus.server.TCPEBusServer;

/**
 *
 * @author Jędrzej Lew
 */
public class Unity3DModeSubscriber {
    private static final Map<Byte, Map<Byte, UCharacter>> charactersInScenes = Collections.synchronizedMap(new HashMap<Byte, Map<Byte, UCharacter>>());
    private static final Map<UCharacter, EventBus> characterBuses = Collections.synchronizedMap(new HashMap<UCharacter, EventBus>());
    private final UnityServer unityServer;
    private final EventBus ebus;
    private final UCharacter character;
    private final User user;
    private byte sceneID;
    private byte characterID;
    
    public Unity3DModeSubscriber(UnityServer unityServer, EventBus ebus, User user, UCharacter character, byte sceneID) {
        this.unityServer = unityServer;
        this.ebus = ebus;
        this.user = user;
        this.character = character;
        this.sceneID = sceneID;
        
        characterID = unityServer.addCharacterToScene(sceneID, character.getName());
        int port = unityServer.getPort()+sceneID;
        
        if(charactersInScenes.containsKey(sceneID)) {
            Map<Byte, UCharacter> charactersInScene = charactersInScenes.get(sceneID);
            charactersInScene.put(characterID, character);
            
            for(byte enemyID : charactersInScene.keySet()) {
                if(characterBuses.containsKey(charactersInScene.get(enemyID))) {
                    characterBuses.get(charactersInScene.get(enemyID)).post(new EnemyArrived(enemyID, charactersInScene.get(enemyID)));
                }
            }
        }
        else {
            Map<Byte, UCharacter> charactersInScene = Collections.synchronizedMap(new HashMap<Byte, UCharacter>());
            charactersInScene.put(characterID, character);
            charactersInScenes.put(sceneID, charactersInScene);
        }
        characterBuses.put(character, ebus);
        ebus.post(new SceneEnterSucceeded(port, characterID, Collections.unmodifiableMap(charactersInScenes.get(sceneID))));
    }
    
    @Subscribe 
    public void ebusClosed(TCPEBusServer.EBusClosed evt) {
        Logger.getLogger(getClass().getName()).info("Ebus of "+user.getLogin()+":"+character.getName()+" closed. Unregistering Unity3DModeSubscriber, removing character from UnityServer");
        unityServer.removeCharacterFromScene(sceneID, character.getName());
        
        if(charactersInScenes.containsKey(sceneID)) {
            Map<Byte, UCharacter> charactersInScene = charactersInScenes.get(sceneID);
            charactersInScene.remove(characterID);
            
            for(byte enemyID : charactersInScene.keySet()) {
                if(characterBuses.containsKey(charactersInScene.get(enemyID)) && enemyID != characterID) {
                    characterBuses.get(charactersInScene.get(enemyID)).post(new EnemyLeft(enemyID, charactersInScene.get(enemyID)));
                }
            }
        }
        
        characterBuses.remove(character);
        
        ebus.unregister(this);
    }
    
    @Subscribe
    public void describeCharacter(DescribeCharacter evt) {
        if(charactersInScenes.containsKey(sceneID)) {
            if(charactersInScenes.get(sceneID).containsKey(evt.characterID)) {
                ebus.post(new CharacterDescription(evt.characterID, charactersInScenes.get(sceneID).get(evt.characterID)));
            }
            else ebus.post(new CouldNotDescribeCharacter(evt.characterID));
        }
        else ebus.post(new CouldNotDescribeCharacter(evt.characterID));
    }
    
    @Subscribe
    public void changeScene(ChangeScene evt) {
        try {
            byte newSceneID = UnityScenes.getSceneID(evt.getSceneName());
            unityServer.removeCharacterFromScene(sceneID, character.getName());
            if(charactersInScenes.containsKey(sceneID)) {
                Map<Byte, UCharacter> charactersInScene = charactersInScenes.get(sceneID);
                charactersInScene.remove(characterID);

                for (byte enemyID : charactersInScene.keySet()) {
                    if (characterBuses.containsKey(charactersInScene.get(enemyID)) && enemyID != characterID) {
                        characterBuses.get(charactersInScene.get(enemyID)).post(new EnemyLeft(enemyID, charactersInScene.get(enemyID)));
                    }
                }
            }
            
            sceneID = newSceneID;
            
            characterID = unityServer.addCharacterToScene(sceneID, character.getName());
            int port = unityServer.getPort() + sceneID;

            if (charactersInScenes.containsKey(sceneID)) {
                Map<Byte, UCharacter> charactersInScene = charactersInScenes.get(sceneID);
                charactersInScene.put(characterID, character);

                for (byte enemyID : charactersInScene.keySet()) {
                    if (characterBuses.containsKey(charactersInScene.get(enemyID))) {
                        characterBuses.get(charactersInScene.get(enemyID)).post(new EnemyArrived(enemyID, charactersInScene.get(enemyID)));
                    }
                }
            } else {
                Map<Byte, UCharacter> charactersInScene = Collections.synchronizedMap(new HashMap<Byte, UCharacter>());
                charactersInScene.put(characterID, character);
                charactersInScenes.put(sceneID, charactersInScene);
            }
            characterBuses.put(character, ebus);
            ebus.post(new SceneEnterSucceeded(port, characterID, Collections.unmodifiableMap(charactersInScenes.get(sceneID))));
        } catch(NoSuchElementException e) {
            ebus.post(new SceneEnterFailed("No such scene on server"));
        }
    }
    
    public static class SceneEnterSucceeded {
        private int port;
        private byte characterID;
        private Map<Byte, UCharacter> enemies;

        public SceneEnterSucceeded() {}
        
        public SceneEnterSucceeded(int port, byte characterID, Map<Byte, UCharacter> enemies) {
            this.port = port;
            this.characterID = characterID;
            this.enemies = enemies;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public byte getCharacterID() {
            return characterID;
        }

        public void setCharacterID(byte characterID) {
            this.characterID = characterID;
        }

        public Map<Byte, UCharacter> getEnemies() {
            return enemies;
        }

        public void setEnemies(Map<Byte, UCharacter> enemies) {
            this.enemies = enemies;
        }
    }
    
    public static class SceneEnterFailed {
        private String message;

        public SceneEnterFailed() {
        }

        public SceneEnterFailed(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    public static class DescribeCharacter {
        private byte characterID;

        public DescribeCharacter(byte characterID) {
            this.characterID = characterID;
        }

        public byte getCharacterID() {
            return characterID;
        }

        public void setCharacterID(byte characterID) {
            this.characterID = characterID;
        }
    }
    
    public static class EnemyArrived {
        private byte characterID;
        private UCharacter character;

        public EnemyArrived() {
        }

        public EnemyArrived(byte characterID, UCharacter character) {
            this.characterID = characterID;
            this.character = character;
        }

        public byte getCharacterID() {
            return characterID;
        }

        public void setCharacterID(byte characterID) {
            this.characterID = characterID;
        }

        public UCharacter getCharacter() {
            return character;
        }

        public void setCharacter(UCharacter character) {
            this.character = character;
        }
    }
    
    public static class EnemyLeft {
        private byte characterID;
        private UCharacter character;

        public EnemyLeft() {
        }

        public EnemyLeft(byte characterID, UCharacter character) {
            this.characterID = characterID;
            this.character = character;
        }

        public byte getCharacterID() {
            return characterID;
        }

        public void setCharacterID(byte characterID) {
            this.characterID = characterID;
        }

        public UCharacter getCharacter() {
            return character;
        }

        public void setCharacter(UCharacter character) {
            this.character = character;
        }
    }
    
    public static class CharacterDescription {
        private byte characterID;
        private UCharacter character;

        public CharacterDescription() {
        }

        public CharacterDescription(byte characterID, UCharacter character) {
            this.characterID = characterID;
            this.character = character;
        }

        public byte getCharacterID() {
            return characterID;
        }

        public void setCharacterID(byte characterID) {
            this.characterID = characterID;
        }

        public UCharacter getCharacter() {
            return character;
        }

        public void setCharacter(UCharacter character) {
            this.character = character;
        }
    }
    
    public static class CouldNotDescribeCharacter {
        private byte characterID;

        public CouldNotDescribeCharacter() {
        }

        public CouldNotDescribeCharacter(byte characterID) {
            this.characterID = characterID;
        }

        public byte getCharacterID() {
            return characterID;
        }

        public void setCharacterID(byte characterID) {
            this.characterID = characterID;
        }
    }
    
    public static class ChangeScene {
        private String sceneName;

        public ChangeScene() {
        }

        public ChangeScene(String sceneName) {
            this.sceneName = sceneName;
        }

        public String getSceneName() {
            return sceneName;
        }

        public void setSceneName(String sceneName) {
            this.sceneName = sceneName;
        }
    }
}
