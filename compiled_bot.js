var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/* Generated from Java with JSweet 2.0.0-SNAPSHOT - http://www.jsweet.org */
var bc19;
(function (bc19) {
    var Utils = (function () {
        function Utils() {
        }
        Utils.canMove = function (r, delta) {
            var passableMap = r.getPassableMap();
            var visibleRobotMap = r.getVisibleRobotMap();
            var newX = r.me.x + delta.x;
            var newY = r.me.y + delta.y;
            if (newX < 0 || newY < 0 || newY > passableMap.length || newX > passableMap[0].length) {
                return false;
            }
            return passableMap[newY][newX] && (visibleRobotMap[newY][newX] <= 0) && Utils.enoughFuelToMove(r, delta.x, delta.y);
        };
        Utils.canMine = function (r) {
            return r.fuel >= Utils.PILGRIM_MINE_FUEL_COST;
        };
        Utils.moveMapThenRandom = function (r, map, radius) {
            var delta = map.getNextMove(radius);
            if (Utils.canMove(r, delta)) {
                return r.move(delta.x, delta.y);
            }
            else {
                if (r.fuel > 5 * Utils.mySpecs(r).FUEL_PER_MOVE) {
                    return Utils.moveRandom(r);
                }
            }
            return null;
        };
        Utils.getSpecs = function (r, unitType) {
            return r.SPECS.UNITS[unitType];
        };
        Utils.mySpecs = function (r) {
            return Utils.getSpecs(r, r.me.unit);
        };
        Utils.moveRandom = function (r) {
            var candidates = Utils.getFreeSpaces(r, (Math.sqrt(Utils.mySpecs(r).SPEED) | 0));
            if (candidates.length === 0) {
                return null;
            }
            while ((candidates.length > 0)) {
                var index = ((Math.random() * candidates.length) | 0);
                var move = candidates[index];
                if (Utils.canMove(r, move)) {
                    return r.move(move.x, move.y);
                }
                /* remove */ candidates.splice(index, 1);
            }
            ;
            return null;
        };
        Utils.getFuelCost = function (r, dx, dy) {
            var rSquared = dx * dx + dy * dy;
            return Utils.mySpecs(r).FUEL_PER_MOVE * rSquared;
        };
        Utils.enoughFuelToMove = function (r, dx, dy) {
            return Utils.getFuelCost(r, dx, dy) <= r.fuel;
        };
        Utils.getFreeSpaces = function (r, range) {
            var passableMap = r.getPassableMap();
            var visibleRobotMap = r.getVisibleRobotMap();
            var freeSpaces = ([]);
            var x = r.me.x;
            var y = r.me.y;
            for (var dx = -range; dx <= range; dx++) {
                for (var dy = -range; dy <= range; dy++) {
                    if (dx * dx + dy * dy > range * range) {
                        continue;
                    }
                    var newX = x + dx;
                    var newY = y + dy;
                    if (newX < 0 || newY < 0 || newY >= passableMap.length || newX >= passableMap[0].length) {
                        continue;
                    }
                    if (passableMap[newY][newX] && visibleRobotMap[newY][newX] <= 0) {
                        /* add */ (freeSpaces.push(new bc19.Point(dx, dy)) > 0);
                    }
                }
                ;
            }
            ;
            return freeSpaces;
        };
        Utils.getAdjacentUnits = function (r, unitType) {
            var nearby = ([]);
            {
                var array122 = r.getVisibleRobots();
                for (var index121 = 0; index121 < array122.length; index121++) {
                    var robot = array122[index121];
                    {
                        if (robot.unit !== unitType) {
                            continue;
                        }
                        if (Math.abs(robot.x - r.me.x) <= 1 && Math.abs(robot.y - r.me.y) <= 1) {
                            /* add */ (nearby.push(new bc19.Point(robot.x - r.me.x, robot.y - r.me.y)) > 0);
                        }
                    }
                }
            }
            return nearby;
        };
        Utils.getUnitsInRange = function (r, unitType, myTeam, minRadius, maxRadius) {
            var nearby = ([]);
            {
                var array124 = r.getVisibleRobots();
                for (var index123 = 0; index123 < array124.length; index123++) {
                    var robot = array124[index123];
                    {
                        if ((robot.unit !== unitType && unitType !== -1) || ((robot.team === r.me.team) === myTeam)) {
                            continue;
                        }
                        var distX = robot.x - r.me.x;
                        var distY = robot.y - r.me.y;
                        var distanceSquared = distX * distX + distY * distY;
                        if (distanceSquared >= minRadius * minRadius && distanceSquared <= maxRadius * maxRadius) {
                            /* add */ (nearby.push(new bc19.Point(robot.x - r.me.x, robot.y - r.me.y)) > 0);
                        }
                    }
                }
            }
            return nearby;
        };
        Utils.getAdjacentFreeSpaces = function (r) {
            var passableMap = r.getPassableMap();
            var visibleRobotMap = r.getVisibleRobotMap();
            var freeSpaces = ([]);
            var x = r.me.x;
            var y = r.me.y;
            var dxes = [-1, 0, 1];
            var dyes = [-1, 0, 1];
            for (var index125 = 0; index125 < dxes.length; index125++) {
                var dx = dxes[index125];
                {
                    for (var index126 = 0; index126 < dyes.length; index126++) {
                        var dy = dyes[index126];
                        {
                            var newX = x + dx;
                            var newY = y + dy;
                            if (passableMap[newY][newX] && visibleRobotMap[newY][newX] <= 0) {
                                /* add */ (freeSpaces.push(new bc19.Point(dx, dy)) > 0);
                            }
                        }
                    }
                }
            }
            return freeSpaces;
        };
        return Utils;
    }());
    Utils.PILGRIM_MINE_FUEL_COST = 1;
    bc19.Utils = Utils;
    Utils["__class"] = "bc19.Utils";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Action = (function () {
        function Action(signal, signalRadius, logs, castleTalk) {
            this.signal = 0;
            this.signal_radius = 0;
            this.logs = null;
            this.castle_talk = 0;
            this.signal = signal;
            this.signal_radius = signalRadius;
            this.logs = logs;
            this.castle_talk = castleTalk;
        }
        return Action;
    }());
    bc19.Action = Action;
    Action["__class"] = "bc19.Action";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Pilgrim = (function () {
        function Pilgrim(myRobot) {
            this.r = null;
            this.r = myRobot;
        }
        Pilgrim.state_$LI$ = function () { if (Pilgrim.state == null)
            Pilgrim.state = Math.random() < 0.5 ? Pilgrim.State.GATHERING_KARB : Pilgrim.State.GATHERING_FUEL; return Pilgrim.state; };
        ;
        Pilgrim.prototype.computeKarbMap = function () {
            var karboniteMap = this.r.getKarboniteMap();
            var targets = ([]);
            for (var y = 0; y < karboniteMap.length; y++) {
                for (var x = 0; x < karboniteMap[y].length; x++) {
                    if (karboniteMap[y][x]) {
                        /* add */ (targets.push(new bc19.Point(x, y)) > 0);
                    }
                }
                ;
            }
            ;
            Pilgrim.karbMap = new bc19.Navigation(this.r, this.r.getPassableMap(), targets);
        };
        Pilgrim.prototype.computeFuelMap = function () {
            var fuelMap = this.r.getFuelMap();
            var targets = ([]);
            for (var y = 0; y < fuelMap.length; y++) {
                for (var x = 0; x < fuelMap[y].length; x++) {
                    if (fuelMap[y][x]) {
                        /* add */ (targets.push(new bc19.Point(x, y)) > 0);
                    }
                }
                ;
            }
            ;
            Pilgrim.fuelsMap = new bc19.Navigation(this.r, this.r.getPassableMap(), targets);
        };
        Pilgrim.prototype.computeCastleMap = function () {
            var targets = ([]);
            {
                var array128 = this.r.getVisibleRobots();
                for (var index127 = 0; index127 < array128.length; index127++) {
                    var robot = array128[index127];
                    {
                        if (robot.unit === this.r.SPECS.CASTLE || robot.unit === this.r.SPECS.CHURCH) {
                            /* add */ (targets.push(new bc19.Point(robot.x, robot.y)) > 0);
                        }
                    }
                }
            }
            Pilgrim.castleMap = new bc19.Navigation(this.r, this.r.getPassableMap(), targets);
        };
        Pilgrim.prototype.computeMaps = function () {
            this.computeKarbMap();
            this.computeFuelMap();
            this.computeCastleMap();
        };
        Pilgrim.prototype.act = function () {
            if (this.r.__turn === 1) {
                this.computeMaps();
            }
            if (this.r.karbonite >= bc19.Utils.getSpecs(this.r, this.r.SPECS.CHURCH).CONSTRUCTION_KARBONITE && this.r.fuel >= bc19.Utils.getSpecs(this.r, this.r.SPECS.CHURCH).CONSTRUCTION_FUEL) {
                var freeSpaces = bc19.Utils.getAdjacentFreeSpaces(this.r);
                var move = freeSpaces[((Math.random() * freeSpaces.length) | 0)];
                return this.r.buildUnit(this.r.SPECS.CHURCH, move.x, move.y);
            }
            if (Pilgrim.state_$LI$() === Pilgrim.State.GATHERING_KARB) {
                if (this.r.getKarboniteMap()[this.r.me.y][this.r.me.x]) {
                    if (this.r.me.karbonite < bc19.Utils.mySpecs(this.r).KARBONITE_CAPACITY) {
                        if (bc19.Utils.canMine(this.r)) {
                            return this.r.mine();
                        }
                    }
                    else {
                        Pilgrim.state = Pilgrim.State.MOVING_RESOURCE_HOME;
                        return this.act();
                    }
                }
                else {
                    return bc19.Utils.moveMapThenRandom(this.r, Pilgrim.karbMap, 1);
                }
            }
            if (Pilgrim.state_$LI$() === Pilgrim.State.GATHERING_FUEL) {
                if (this.r.getFuelMap()[this.r.me.y][this.r.me.x]) {
                    if (this.r.me.fuel < bc19.Utils.mySpecs(this.r).FUEL_CAPACITY) {
                        if (bc19.Utils.canMine(this.r)) {
                            return this.r.mine();
                        }
                    }
                    else {
                        Pilgrim.state = Pilgrim.State.MOVING_RESOURCE_HOME;
                        return this.act();
                    }
                }
                else {
                    return bc19.Utils.moveMapThenRandom(this.r, Pilgrim.fuelsMap, 1);
                }
            }
            if (Pilgrim.state_$LI$() === Pilgrim.State.MOVING_RESOURCE_HOME) {
                var adjacentCastles = bc19.Utils.getAdjacentUnits(this.r, this.r.SPECS.CASTLE);
                if (adjacentCastles.length > 0) {
                    if (this.r.me.karbonite > 0) {
                        var adjacentCastle = adjacentCastles[0];
                        return this.r.give(adjacentCastle.x, adjacentCastle.y, this.r.me.karbonite, this.r.me.fuel);
                    }
                    else {
                        Pilgrim.state = Math.random() < 0.2 ? Pilgrim.State.GATHERING_KARB : Pilgrim.State.GATHERING_FUEL;
                        return this.act();
                    }
                }
                else {
                    return bc19.Utils.moveMapThenRandom(this.r, Pilgrim.castleMap, 1);
                }
            }
            return null;
        };
        return Pilgrim;
    }());
    Pilgrim.karbMap = null;
    Pilgrim.fuelsMap = null;
    Pilgrim.castleMap = null;
    bc19.Pilgrim = Pilgrim;
    Pilgrim["__class"] = "bc19.Pilgrim";
    Pilgrim["__interfaces"] = ["bc19.BCRobot"];
    (function (Pilgrim) {
        var State;
        (function (State) {
            State[State["GATHERING_KARB"] = 0] = "GATHERING_KARB";
            State[State["GATHERING_FUEL"] = 1] = "GATHERING_FUEL";
            State[State["MOVING_RESOURCE_HOME"] = 2] = "MOVING_RESOURCE_HOME";
        })(State = Pilgrim.State || (Pilgrim.State = {}));
    })(Pilgrim = bc19.Pilgrim || (bc19.Pilgrim = {}));
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Castle = (function () {
        function Castle(myRobot) {
            this.r = null;
            this.r = myRobot;
        }
        Castle.prototype.act = function () {
            if (this.r.karbonite > bc19.Utils.getSpecs(this.r, this.r.SPECS.PILGRIM).CONSTRUCTION_KARBONITE && this.r.fuel > bc19.Utils.getSpecs(this.r, this.r.SPECS.PILGRIM).CONSTRUCTION_FUEL && Castle.pilgrimsBuilt < Castle.MAX_PILGRIMS) {
                var freeSpaces = bc19.Utils.getAdjacentFreeSpaces(this.r);
                var move = freeSpaces[((Math.random() * freeSpaces.length) | 0)];
                Castle.pilgrimsBuilt += 1;
                return this.r.buildUnit(this.r.SPECS.PILGRIM, move.x, move.y);
            }
            return null;
        };
        return Castle;
    }());
    Castle.MAX_PILGRIMS = 5;
    Castle.pilgrimsBuilt = 0;
    bc19.Castle = Castle;
    Castle["__class"] = "bc19.Castle";
    Castle["__interfaces"] = ["bc19.BCRobot"];
})(bc19 || (bc19 = {}));
(function (bc19) {
    var BCException = (function (_super) {
        __extends(BCException, _super);
        function BCException(errorMessage) {
            var _this = _super.call(this, errorMessage) || this;
            _this.message = errorMessage;
            Object.setPrototypeOf(_this, BCException.prototype);
            return _this;
        }
        return BCException;
    }(Error));
    bc19.BCException = BCException;
    BCException["__class"] = "bc19.BCException";
    BCException["__interfaces"] = ["java.io.Serializable"];
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Point = (function () {
        function Point(x, y) {
            this.x = 0;
            this.y = 0;
            this.x = x;
            this.y = y;
        }
        Point.prototype.getX = function () {
            return this.x;
        };
        Point.prototype.getY = function () {
            return this.y;
        };
        return Point;
    }());
    bc19.Point = Point;
    Point["__class"] = "bc19.Point";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var BCAbstractRobot = (function () {
        function BCAbstractRobot() {
            this.SPECS = null;
            this.gameState = null;
            this.logs = null;
            this.__signal = 0;
            this.signalRadius = 0;
            this.__castleTalk = 0;
            this.me = null;
            this.id = 0;
            this.fuel = 0;
            this.karbonite = 0;
            this.lastOffer = null;
            this.map = null;
            this.karboniteMap = null;
            this.fuelMap = null;
            this.resetState();
        }
        BCAbstractRobot.prototype.setSpecs = function (specs) {
            this.SPECS = specs;
        };
        /*private*/ BCAbstractRobot.prototype.resetState = function () {
            this.logs = ([]);
            this.__signal = 0;
            this.signalRadius = 0;
            this.__castleTalk = 0;
        };
        BCAbstractRobot.prototype._do_turn = function (gameState) {
            this.gameState = gameState;
            this.id = gameState.id;
            this.karbonite = gameState.karbonite;
            this.fuel = gameState.fuel;
            this.lastOffer = gameState.last_offer;
            this.me = this.getRobot(this.id);
            if (this.me.turn === 1) {
                this.map = gameState.map;
                this.karboniteMap = gameState.karbonite_map;
                this.fuelMap = gameState.fuel_map;
            }
            var t = null;
            try {
                t = this.turn();
            }
            catch (e) {
                t = new bc19.ErrorAction(e, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
            }
            ;
            if (t == null)
                t = new bc19.Action(this.__signal, this.signalRadius, this.logs, this.__castleTalk);
            t.signal = this.__signal;
            t.signal_radius = this.signalRadius;
            t.logs = this.logs;
            t.castle_talk = this.__castleTalk;
            this.resetState();
            return t;
        };
        /*private*/ BCAbstractRobot.prototype.checkOnMap = function (x, y) {
            return x >= 0 && x < this.gameState.shadow[0].length && y >= 0 && y < this.gameState.shadow.length;
        };
        BCAbstractRobot.prototype.log = function (message) {
            /* add */ (this.logs.push(message) > 0);
        };
        BCAbstractRobot.prototype.signal = function (value, radius) {
            if (this.fuel < radius)
                throw new bc19.BCException("Not enough fuel to signal given radius.");
            if (value < 0 || value >= Math.pow(2, this.SPECS.COMMUNICATION_BITS))
                throw new bc19.BCException("Invalid signal, must be within bit range.");
            if (radius > 2 * Math.pow(this.SPECS.MAX_BOARD_SIZE - 1, 2))
                throw new bc19.BCException("Signal radius is too big.");
            this.__signal = value;
            this.signalRadius = radius;
            this.fuel -= radius;
        };
        BCAbstractRobot.prototype.castleTalk = function (value) {
            if (value < 0 || value >= Math.pow(2, this.SPECS.CASTLE_TALK_BITS))
                throw new bc19.BCException("Invalid castle talk, must be between 0 and 2^8.");
            this.__castleTalk = value;
        };
        BCAbstractRobot.prototype.proposeTrade = function (k, f) {
            if (this.me.unit !== this.SPECS.CASTLE)
                throw new bc19.BCException("Only castles can trade.");
            if (Math.abs(k) >= this.SPECS.MAX_TRADE || Math.abs(f) >= this.SPECS.MAX_TRADE)
                throw new bc19.BCException("Cannot trade over " + ('' + (this.SPECS.MAX_TRADE)) + " in a given turn.");
            return new bc19.TradeAction(f, k, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.buildUnit = function (unit, dx, dy) {
            if (this.me.unit !== this.SPECS.PILGRIM && this.me.unit !== this.SPECS.CASTLE && this.me.unit !== this.SPECS.CHURCH)
                throw new bc19.BCException("This unit type cannot build.");
            if (this.me.unit === this.SPECS.PILGRIM && unit !== this.SPECS.CHURCH)
                throw new bc19.BCException("Pilgrims can only build churches.");
            if (this.me.unit !== this.SPECS.PILGRIM && unit === this.SPECS.CHURCH)
                throw new bc19.BCException("Only pilgrims can build churches.");
            if (dx < -1 || dy < -1 || dx > 1 || dy > 1)
                throw new bc19.BCException("Can only build in adjacent squares.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t build units off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] !== 0)
                throw new bc19.BCException("Cannot build on occupied tile.");
            if (!this.map[this.me.y + dy][this.me.x + dx])
                throw new bc19.BCException("Cannot build onto impassable terrain.");
            if (this.karbonite < this.SPECS.UNITS[unit].CONSTRUCTION_KARBONITE || this.fuel < this.SPECS.UNITS[unit].CONSTRUCTION_FUEL)
                throw new bc19.BCException("Cannot afford to build specified unit.");
            return new bc19.BuildAction(unit, dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.move = function (dx, dy) {
            if (this.me.unit === this.SPECS.CASTLE || this.me.unit === this.SPECS.CHURCH)
                throw new bc19.BCException("Churches and Castles cannot move.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t move off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] === -1)
                throw new bc19.BCException("Cannot move outside of vision range.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] !== 0)
                throw new bc19.BCException("Cannot move onto occupied tile.");
            if (!this.map[this.me.y + dy][this.me.x + dx])
                throw new bc19.BCException("Cannot move onto impassable terrain.");
            var r = dx * dx + dy * dy;
            if (r > this.SPECS.UNITS[this.me.unit].SPEED)
                throw new bc19.BCException("Slow down, cowboy.  Tried to move faster than unit can.");
            if (this.fuel < r * this.SPECS.UNITS[this.me.unit].FUEL_PER_MOVE)
                throw new bc19.BCException("Not enough fuel to move at given speed.");
            return new bc19.MoveAction(dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.mine = function () {
            if (this.me.unit !== this.SPECS.PILGRIM)
                throw new bc19.BCException("Only Pilgrims can mine.");
            if (this.fuel < this.SPECS.MINE_FUEL_COST)
                throw new bc19.BCException("Not enough fuel to mine.");
            if (this.karboniteMap[this.me.y][this.me.x]) {
                if (this.me.karbonite >= this.SPECS.UNITS[this.SPECS.PILGRIM].KARBONITE_CAPACITY)
                    throw new bc19.BCException("Cannot mine, as at karbonite capacity.");
            }
            else if (this.fuelMap[this.me.y][this.me.x]) {
                if (this.me.fuel >= this.SPECS.UNITS[this.SPECS.PILGRIM].FUEL_CAPACITY)
                    throw new bc19.BCException("Cannot mine, as at fuel capacity.");
            }
            else
                throw new bc19.BCException("Cannot mine square without fuel or karbonite.");
            return new bc19.MineAction(this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.give = function (dx, dy, k, f) {
            if (dx > 1 || dx < -1 || dy > 1 || dy < -1 || (dx === 0 && dy === 0))
                throw new bc19.BCException("Can only give to adjacent squares.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t give off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] <= 0)
                throw new bc19.BCException("Cannot give to empty square.");
            if (k < 0 || f < 0 || this.me.karbonite < k || this.me.fuel < f)
                throw new bc19.BCException("Do not have specified amount to give.");
            return new bc19.GiveAction(k, f, dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.attack = function (dx, dy) {
            if (this.me.unit !== this.SPECS.CRUSADER && this.me.unit !== this.SPECS.PREACHER && this.me.unit !== this.SPECS.PROPHET)
                throw new bc19.BCException("Given unit cannot attack.");
            if (this.fuel < this.SPECS.UNITS[this.me.unit].ATTACK_FUEL_COST)
                throw new bc19.BCException("Not enough fuel to attack.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t attack off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] === -1)
                throw new bc19.BCException("Cannot attack outside of vision range.");
            if (!this.map[this.me.y + dy][this.me.x + dx])
                throw new bc19.BCException("Cannot attack impassable terrain.");
            var r = dx * dx + dy * dy;
            if (r > this.SPECS.UNITS[this.me.unit].ATTACK_RADIUS[1] || r < this.SPECS.UNITS[this.me.unit].ATTACK_RADIUS[0])
                throw new bc19.BCException("Cannot attack outside of attack range.");
            return new bc19.AttackAction(dx, dy, this.__signal, this.signalRadius, this.logs, this.__castleTalk);
        };
        BCAbstractRobot.prototype.getRobot = function (id) {
            if (id <= 0)
                return null;
            for (var i = 0; i < this.gameState.visible.length; i++) {
                if (this.gameState.visible[i].id === id) {
                    return this.gameState.visible[i];
                }
            }
            ;
            return null;
        };
        BCAbstractRobot.prototype.isVisible = function (robot) {
            for (var x = 0; x < this.gameState.shadow[0].length; x++) {
                for (var y = 0; y < this.gameState.shadow.length; y++) {
                    if (robot.id === this.gameState.shadow[y][x])
                        return true;
                }
                ;
            }
            ;
            return false;
        };
        BCAbstractRobot.prototype.isRadioing = function (robot) {
            return robot.signal >= 0;
        };
        BCAbstractRobot.prototype.getVisibleRobotMap = function () {
            return this.gameState.shadow;
        };
        BCAbstractRobot.prototype.getPassableMap = function () {
            return this.map;
        };
        BCAbstractRobot.prototype.getKarboniteMap = function () {
            return this.karboniteMap;
        };
        BCAbstractRobot.prototype.getFuelMap = function () {
            return this.fuelMap;
        };
        BCAbstractRobot.prototype.getVisibleRobots = function () {
            return this.gameState.visible;
        };
        BCAbstractRobot.prototype.turn = function () {
            return null;
        };
        return BCAbstractRobot;
    }());
    bc19.BCAbstractRobot = BCAbstractRobot;
    BCAbstractRobot["__class"] = "bc19.BCAbstractRobot";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Church = (function () {
        function Church(myRobot) {
            this.r = null;
            this.r = myRobot;
        }
        Church.prototype.act = function () {
            if (this.r.karbonite > bc19.Utils.getSpecs(this.r, this.r.SPECS.PILGRIM).CONSTRUCTION_KARBONITE && this.r.fuel > bc19.Utils.getSpecs(this.r, this.r.SPECS.PILGRIM).CONSTRUCTION_FUEL && Church.pilgrimsBuilt < Church.MAX_PILGRIMS) {
                var freeSpaces = bc19.Utils.getAdjacentFreeSpaces(this.r);
                var move = freeSpaces[((Math.random() * freeSpaces.length) | 0)];
                Church.pilgrimsBuilt += 1;
                return this.r.buildUnit(this.r.SPECS.PILGRIM, move.x, move.y);
            }
            return null;
        };
        return Church;
    }());
    Church.MAX_PILGRIMS = 5;
    Church.pilgrimsBuilt = 0;
    bc19.Church = Church;
    Church["__class"] = "bc19.Church";
    Church["__interfaces"] = ["bc19.BCRobot"];
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Queue = (function () {
        function Queue() {
            this.total = 0;
            this.first = null;
            this.last = null;
        }
        Queue.prototype.enqueue = function (ele) {
            var current = this.last;
            this.last = new Queue.Node(this);
            this.last.ele = ele;
            if (this.total++ === 0)
                this.first = this.last;
            else
                current.next = this.last;
            return this;
        };
        Queue.prototype.dequeue = function () {
            if (this.total === 0)
                throw Object.defineProperty(new Error(), '__classes', { configurable: true, value: ['java.lang.Throwable', 'java.lang.Object', 'java.lang.RuntimeException', 'java.util.NoSuchElementException', 'java.lang.Exception'] });
            var ele = this.first.ele;
            this.first = this.first.next;
            if (--this.total === 0)
                this.last = null;
            return ele;
        };
        Queue.prototype.isEmpty = function () {
            return this.total === 0;
        };
        /**
         *
         * @return {string}
         */
        Queue.prototype.toString = function () {
            var sb = { str: "", toString: function () { return this.str; } };
            var tmp = this.first;
            while ((tmp != null)) {
                /* append */ (function (sb) { sb.str = sb.str.concat(", "); return sb; })(/* append */ (function (sb) { sb.str = sb.str.concat(tmp.ele); return sb; })(sb));
                tmp = tmp.next;
            }
            ;
            return sb.str;
        };
        return Queue;
    }());
    bc19.Queue = Queue;
    Queue["__class"] = "bc19.Queue";
    (function (Queue) {
        var Node = (function () {
            function Node(__parent) {
                this.__parent = __parent;
                this.ele = null;
                this.next = null;
            }
            return Node;
        }());
        Queue.Node = Node;
        Node["__class"] = "bc19.Queue.Node";
    })(Queue = bc19.Queue || (bc19.Queue = {}));
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Prophet = (function () {
        function Prophet(myRobot) {
            this.r = null;
            this.r = myRobot;
        }
        Prophet.prototype.act = function () {
            return null;
        };
        return Prophet;
    }());
    bc19.Prophet = Prophet;
    Prophet["__class"] = "bc19.Prophet";
    Prophet["__interfaces"] = ["bc19.BCRobot"];
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Navigation = (function () {
        function Navigation(r, passableMap, targets, maxDistance) {
            var _this = this;
            if (((r != null && r instanceof bc19.MyRobot) || r === null) && ((passableMap != null && passableMap instanceof Array && (passableMap.length == 0 || passableMap[0] == null || passableMap[0] instanceof Array)) || passableMap === null) && ((targets != null && (targets instanceof Array)) || targets === null) && ((typeof maxDistance === 'number') || maxDistance === null)) {
                var __args = Array.prototype.slice.call(arguments);
                this.r = null;
                this.passableMap = null;
                this.targets = null;
                this.maxDistance = 0;
                this.distances = null;
                this.r = null;
                this.passableMap = null;
                this.targets = null;
                this.maxDistance = 0;
                this.distances = null;
                (function () {
                    _this.r = r;
                    _this.passableMap = passableMap;
                    _this.maxDistance = maxDistance;
                    _this.distances = (function (dims) { var allocate = function (dims) { if (dims.length == 0) {
                        return 0;
                    }
                    else {
                        var array = [];
                        for (var i = 0; i < dims[0]; i++) {
                            array.push(allocate(dims.slice(1)));
                        }
                        return array;
                    } }; return allocate(dims); })([passableMap.length, passableMap[0].length]);
                    _this.targets = targets;
                    _this.recalculateDistanceMap();
                })();
            }
            else if (((r != null && r instanceof bc19.MyRobot) || r === null) && ((passableMap != null && passableMap instanceof Array && (passableMap.length == 0 || passableMap[0] == null || passableMap[0] instanceof Array)) || passableMap === null) && ((targets != null && (targets instanceof Array)) || targets === null) && maxDistance === undefined) {
                var __args = Array.prototype.slice.call(arguments);
                {
                    var __args_1 = Array.prototype.slice.call(arguments);
                    var maxDistance_1 = Number.MAX_VALUE;
                    this.r = null;
                    this.passableMap = null;
                    this.targets = null;
                    this.maxDistance = 0;
                    this.distances = null;
                    this.r = null;
                    this.passableMap = null;
                    this.targets = null;
                    this.maxDistance = 0;
                    this.distances = null;
                    (function () {
                        _this.r = r;
                        _this.passableMap = passableMap;
                        _this.maxDistance = maxDistance_1;
                        _this.distances = (function (dims) { var allocate = function (dims) { if (dims.length == 0) {
                            return 0;
                        }
                        else {
                            var array = [];
                            for (var i = 0; i < dims[0]; i++) {
                                array.push(allocate(dims.slice(1)));
                            }
                            return array;
                        } }; return allocate(dims); })([passableMap.length, passableMap[0].length]);
                        _this.targets = targets;
                        _this.recalculateDistanceMap();
                    })();
                }
            }
            else
                throw new Error('invalid overload');
        }
        Navigation.prototype.printDistances = function () {
            for (var i = 0; i < this.distances.length; i++) {
                var thing = "";
                for (var j = 0; j < this.distances[0].length; j++) {
                    if (this.distances[i][j] < 10000) {
                        thing += (this.distances[i][j] + " ");
                    }
                    else {
                        thing += "inf";
                    }
                }
                ;
                this.r.log(thing);
            }
            ;
        };
        Navigation.prototype.setThreshold = function (threshold) {
            this.maxDistance = threshold;
        };
        /*private*/ Navigation.prototype.getPossibleMovementDeltas = function (maxMovementR) {
            var deltas = ([]);
            for (var dx = -maxMovementR; dx <= maxMovementR; dx++) {
                for (var dy = -maxMovementR; dy <= maxMovementR; dy++) {
                    if (dx === 0 && dy === 0) {
                        continue;
                    }
                    if (dx * dx + dy * dy > maxMovementR * maxMovementR) {
                        continue;
                    }
                    /* add */ (deltas.push(new bc19.Point(dx, dy)) > 0);
                }
                ;
            }
            ;
            return deltas;
        };
        /*private*/ Navigation.prototype.getAdjacentDeltas = function () {
            var deltas = ([]);
            var dxes = [-1, 0, 1];
            var dyes = [-1, 0, 1];
            for (var index129 = 0; index129 < dxes.length; index129++) {
                var dx = dxes[index129];
                {
                    for (var index130 = 0; index130 < dyes.length; index130++) {
                        var dy = dyes[index130];
                        {
                            /* add */ (deltas.push(new bc19.Point(dx, dy)) > 0);
                        }
                    }
                }
            }
            return deltas;
        };
        Navigation.prototype.recalculateDistanceMap = function () {
            var movementDeltas = this.getAdjacentDeltas();
            for (var i = 0; i < this.distances.length; i++) {
                for (var j = 0; j < this.distances[0].length; j++) {
                    this.distances[i][j] = Number.MAX_VALUE;
                }
                ;
            }
            ;
            var queue = (new bc19.Queue());
            for (var index131 = 0; index131 < this.targets.length; index131++) {
                var target = this.targets[index131];
                {
                    this.distances[target.y][target.x] = 0;
                    queue.enqueue(new bc19.Point(target.x, target.y));
                }
            }
            while ((!queue.isEmpty())) {
                var loc = queue.dequeue();
                var curDistance = this.distances[loc.y][loc.x];
                for (var index132 = 0; index132 < movementDeltas.length; index132++) {
                    var disp = movementDeltas[index132];
                    {
                        var newX = loc.getX() + disp.x;
                        var newY = loc.getY() + disp.y;
                        if (newX < 0 || newY < 0 || newY >= this.distances.length || newX >= this.distances[0].length) {
                            continue;
                        }
                        if (!this.passableMap[newY][newX]) {
                            continue;
                        }
                        var newDistance = 1 + curDistance;
                        if (newDistance >= this.maxDistance) {
                            continue;
                        }
                        if (this.distances[newY][newX] <= newDistance) {
                            continue;
                        }
                        this.distances[newY][newX] = newDistance;
                        queue.enqueue(new bc19.Point(newX, newY));
                    }
                }
            }
            ;
        };
        /**
         * Returns a delta to move according to a start location and radius (not r_squared, just r)
         * <p>
         * Tries all possible directions, returning the best one we can move towards.
         * <p>
         * Uses some sort of heuristic to weight moving quick against using fuel.
         * <p>
         * Null is returned if all adjacent squares are 'too far' (over threshold)
         * or impossible to reach.
         * @param {number} radius
         * @return {bc19.Point}
         */
        Navigation.prototype.getNextMove = function (radius) {
            var possibleDeltas = this.getPossibleMovementDeltas(radius);
            var minDist = Number.MAX_VALUE;
            var bestDelta = null;
            var start = new bc19.Point(this.r.me.x, this.r.me.y);
            for (var index133 = 0; index133 < possibleDeltas.length; index133++) {
                var delta = possibleDeltas[index133];
                {
                    var newX = start.x + delta.x;
                    var newY = start.y + delta.y;
                    if (newX > -1 && newY > -1 && newY < this.distances.length && newX < this.distances[newY].length && this.distances[newY][newX] < minDist) {
                        bestDelta = delta;
                        minDist = this.distances[newY][newX];
                    }
                }
            }
            return bestDelta;
        };
        Navigation.prototype.getDijkstraMapValue = function (location) {
            var x = location.x;
            var y = location.y;
            if (x >= -1 && y >= -1 && y < this.distances.length && x < this.distances[y].length) {
                return this.distances[y][x];
            }
            return Number.MIN_VALUE;
        };
        Navigation.prototype.addTarget = function (pos) {
            /* add */ (this.targets.push(pos) > 0);
        };
        Navigation.prototype.removeTarget = function (pos) {
            /* remove */ (function (a) { return a.splice(a.indexOf(pos), 1); })(this.targets);
        };
        Navigation.prototype.clearTargets = function () {
            /* clear */ (this.targets.length = 0);
        };
        Navigation.prototype.getTargets = function () {
            return this.targets;
        };
        return Navigation;
    }());
    bc19.Navigation = Navigation;
    Navigation["__class"] = "bc19.Navigation";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var MineAction = (function (_super) {
        __extends(MineAction, _super);
        function MineAction(signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.action = "mine";
            return _this;
        }
        return MineAction;
    }(bc19.Action));
    bc19.MineAction = MineAction;
    MineAction["__class"] = "bc19.MineAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var ErrorAction = (function (_super) {
        __extends(ErrorAction, _super);
        function ErrorAction(error, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.error = null;
            _this.error = error.message;
            return _this;
        }
        return ErrorAction;
    }(bc19.Action));
    bc19.ErrorAction = ErrorAction;
    ErrorAction["__class"] = "bc19.ErrorAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var GiveAction = (function (_super) {
        __extends(GiveAction, _super);
        function GiveAction(giveKarbonite, giveFuel, dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.give_karbonite = 0;
            _this.give_fuel = 0;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "give";
            _this.give_karbonite = giveKarbonite;
            _this.give_fuel = giveFuel;
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return GiveAction;
    }(bc19.Action));
    bc19.GiveAction = GiveAction;
    GiveAction["__class"] = "bc19.GiveAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var BuildAction = (function (_super) {
        __extends(BuildAction, _super);
        function BuildAction(buildUnit, dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.build_unit = 0;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "build";
            _this.build_unit = buildUnit;
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return BuildAction;
    }(bc19.Action));
    bc19.BuildAction = BuildAction;
    BuildAction["__class"] = "bc19.BuildAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var TradeAction = (function (_super) {
        __extends(TradeAction, _super);
        function TradeAction(trade_fuel, trade_karbonite, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.trade_fuel = 0;
            _this.trade_karbonite = 0;
            _this.action = "trade";
            _this.trade_fuel = trade_fuel;
            _this.trade_karbonite = trade_karbonite;
            return _this;
        }
        return TradeAction;
    }(bc19.Action));
    bc19.TradeAction = TradeAction;
    TradeAction["__class"] = "bc19.TradeAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var MoveAction = (function (_super) {
        __extends(MoveAction, _super);
        function MoveAction(dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "move";
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return MoveAction;
    }(bc19.Action));
    bc19.MoveAction = MoveAction;
    MoveAction["__class"] = "bc19.MoveAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var AttackAction = (function (_super) {
        __extends(AttackAction, _super);
        function AttackAction(dx, dy, signal, signalRadius, logs, castleTalk) {
            var _this = _super.call(this, signal, signalRadius, logs, castleTalk) || this;
            _this.action = null;
            _this.dx = 0;
            _this.dy = 0;
            _this.action = "attack";
            _this.dx = dx;
            _this.dy = dy;
            return _this;
        }
        return AttackAction;
    }(bc19.Action));
    bc19.AttackAction = AttackAction;
    AttackAction["__class"] = "bc19.AttackAction";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var MyRobot = (function (_super) {
        __extends(MyRobot, _super);
        function MyRobot() {
            var _this = _super.call(this) || this;
            _this.__turn = 0;
            return _this;
        }
        MyRobot.prototype.turn = function () {
            this.doAllUnitActions();
            var robot = null;
            if (this.me.unit === this.SPECS.CASTLE) {
                robot = new bc19.Castle(this);
            }
            else if (this.me.unit === this.SPECS.PILGRIM) {
                robot = new bc19.Pilgrim(this);
            }
            else if (this.me.unit === this.SPECS.CHURCH) {
                robot = new bc19.Castle(this);
            }
            else if (this.me.unit === this.SPECS.CRUSADER) {
            }
            else if (this.me.unit === this.SPECS.PROPHET) {
            }
            else if (this.me.unit === this.SPECS.PREACHER) {
            }
            return robot.act();
        };
        /*private*/ MyRobot.prototype.doAllUnitActions = function () {
            this.__turn++;
        };
        return MyRobot;
    }(bc19.BCAbstractRobot));
    bc19.MyRobot = MyRobot;
    MyRobot["__class"] = "bc19.MyRobot";
})(bc19 || (bc19 = {}));
bc19.Pilgrim.state_$LI$();
//# sourceMappingURL=bundle.js.map
var specs = {"COMMUNICATION_BITS":16,"CASTLE_TALK_BITS":8,"MAX_ROUNDS":1000,"TRICKLE_FUEL":25,"INITIAL_KARBONITE":100,"INITIAL_FUEL":500,"MINE_FUEL_COST":1,"KARBONITE_YIELD":2,"FUEL_YIELD":10,"MAX_TRADE":1024,"MAX_BOARD_SIZE":64,"MAX_ID":4096,"CASTLE":0,"CHURCH":1,"PILGRIM":2,"CRUSADER":3,"PROPHET":4,"PREACHER":5,"RED":0,"BLUE":1,"CHESS_INITIAL":100,"CHESS_EXTRA":20,"TURN_MAX_TIME":200,"MAX_MEMORY":50000000,"UNITS":[{"CONSTRUCTION_KARBONITE":null,"CONSTRUCTION_FUEL":null,"KARBONITE_CAPACITY":null,"FUEL_CAPACITY":null,"SPEED":0,"FUEL_PER_MOVE":null,"STARTING_HP":100,"VISION_RADIUS":100,"ATTACK_DAMAGE":null,"ATTACK_RADIUS":null,"ATTACK_FUEL_COST":null,"DAMAGE_SPREAD":null},{"CONSTRUCTION_KARBONITE":50,"CONSTRUCTION_FUEL":200,"KARBONITE_CAPACITY":null,"FUEL_CAPACITY":null,"SPEED":0,"FUEL_PER_MOVE":null,"STARTING_HP":50,"VISION_RADIUS":100,"ATTACK_DAMAGE":null,"ATTACK_RADIUS":null,"ATTACK_FUEL_COST":null,"DAMAGE_SPREAD":null},{"CONSTRUCTION_KARBONITE":10,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":1,"STARTING_HP":10,"VISION_RADIUS":100,"ATTACK_DAMAGE":null,"ATTACK_RADIUS":null,"ATTACK_FUEL_COST":null,"DAMAGE_SPREAD":null},{"CONSTRUCTION_KARBONITE":20,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":9,"FUEL_PER_MOVE":1,"STARTING_HP":40,"VISION_RADIUS":36,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[1,16],"ATTACK_FUEL_COST":10,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":25,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":2,"STARTING_HP":20,"VISION_RADIUS":64,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[16,64],"ATTACK_FUEL_COST":25,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":30,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":3,"STARTING_HP":60,"VISION_RADIUS":16,"ATTACK_DAMAGE":20,"ATTACK_RADIUS":[1,16],"ATTACK_FUEL_COST":15,"DAMAGE_SPREAD":3}]};
var robot = new bc19.MyRobot(); robot.setSpecs(specs);