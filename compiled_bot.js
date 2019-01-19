var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/* Generated from Java with JSweet 2.0.0-SNAPSHOT - http://www.jsweet.org */
var bc19;
(function (bc19) {
    /**
     * Created by patil215 on 1/18/19.
     * @class
     */
    var Crusader = (function () {
        function Crusader() {
        }
        Crusader.act = function (r) {
            return null;
        };
        return Crusader;
    }());
    bc19.Crusader = Crusader;
    Crusader["__class"] = "bc19.Crusader";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Utils = (function () {
        function Utils() {
        }
        Utils.findClosestPoint = function (r, points) {
            var bestPoint = null;
            var bestDistance = 100000;
            for (var index121 = 0; index121 < points.length; index121++) {
                var point = points[index121];
                {
                    var newDistance = Math.abs(point.x - r.me.x) + Math.abs(point.y - r.me.y);
                    if (newDistance < bestDistance) {
                        bestDistance = newDistance;
                        bestPoint = point;
                    }
                }
            }
            return bestPoint;
        };
        Utils.isNearbySpaceEmpty = function (r, delta) {
            var passableMap = r.getPassableMap();
            var visibleRobotMap = r.getVisibleRobotMap();
            var newX = r.me.x + delta.x;
            var newY = r.me.y + delta.y;
            if (newX < 0 || newY < 0 || newY >= passableMap.length || newX >= passableMap[0].length) {
                return false;
            }
            return passableMap[newY][newX] && (visibleRobotMap[newY][newX] <= 0);
        };
        Utils.canMove = function (r, delta) {
            return Utils.isNearbySpaceEmpty(r, delta) && Utils.enoughFuelToMove(r, delta.x, delta.y);
        };
        Utils.canMine = function (r) {
            return r.fuel >= bc19.Constants.PILGRIM_MINE_FUEL_COST;
        };
        Utils.canAttack = function (r, delta) {
            var dx = delta.x;
            var dy = delta.y;
            var rSquared = dx * dx + dy * dy;
            return rSquared >= Utils.mySpecs(r).ATTACK_RADIUS[0] && rSquared <= Utils.mySpecs(r).ATTACK_RADIUS[1] && r.fuel >= Utils.mySpecs(r).ATTACK_FUEL_COST;
        };
        Utils.canSignal = function (r, radiusSq) {
            return r.fuel >= Math.ceil(Math.sqrt(radiusSq));
        };
        Utils.canBuild = function (r, unitToBuild) {
            return r.karbonite >= Utils.getSpecs(r, unitToBuild).CONSTRUCTION_KARBONITE && r.fuel >= Utils.getSpecs(r, unitToBuild).CONSTRUCTION_FUEL;
        };
        Utils.tryAndAttack = function (r, attackRadiusSq) {
            var enemiesNearby = Utils.getRobotSortInRange(r, false, 0, attackRadiusSq);
            if (enemiesNearby.length > 0) {
                r.log("********************************************");
                for (var index122 = 0; index122 < enemiesNearby.length; index122++) {
                    var target = enemiesNearby[index122];
                    {
                        var attackPoint = new bc19.Point(target.x - r.me.x, target.y - r.me.y);
                        if (Utils.canAttack(r, attackPoint)) {
                            return r.attack(attackPoint.x, attackPoint.y);
                        }
                    }
                }
            }
            return null;
        };
        Utils.tryAndBuildInRandomSpace = function (r, unitToBuild) {
            var freeSpaces = Utils.getAdjacentFreeSpaces(r);
            if (freeSpaces.length === 0) {
                return null;
            }
            var location = freeSpaces[((Math.random() * freeSpaces.length) | 0)];
            if (Utils.canBuild(r, unitToBuild)) {
                return r.buildUnit(unitToBuild, location.x, location.y);
            }
            return null;
        };
        Utils.moveDijkstra = function (r, map, radius) {
            var delta = map.getNextMove(radius);
            if (delta != null) {
                return r.move(delta.x, delta.y);
            }
            return null;
        };
        Utils.moveDijkstraThenRandom = function (r, map, radius) {
            var action = Utils.moveDijkstra(r, map, radius);
            if (action != null) {
                return action;
            }
            if (r.fuel > 5 * Utils.mySpecs(r).FUEL_PER_MOVE) {
                return Utils.moveRandom(r);
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
            return r.fuel >= Utils.getFuelCost(r, dx, dy);
        };
        Utils.getFreeSpaces = function (r, range) {
            var freeSpaces = ([]);
            for (var dx = -range; dx <= range; dx++) {
                for (var dy = -range; dy <= range; dy++) {
                    if (dx * dx + dy * dy > range * range) {
                        continue;
                    }
                    var delta = new bc19.Point(dx, dy);
                    if (Utils.isNearbySpaceEmpty(r, delta)) {
                        /* add */ (freeSpaces.push(delta) > 0);
                    }
                }
                ;
            }
            ;
            return freeSpaces;
        };
        Utils.isOn = function (r, other) {
            return r.me.x === other.x && r.me.y === other.y;
        };
        Utils.isAdjacentOrOn = function (r, other) {
            return Math.abs(r.me.x - other.x) <= 1 && Math.abs(r.me.y - other.y) <= 1;
        };
        Utils.isBetween = function (a, b, test) {
            return (b.x - a.x) * (test.x - a.x) + (b.y - a.y) * (test.y - a.y) >= 1;
        };
        Utils.getAdjacentUnits = function (r, unitType, myTeam) {
            var nearby = ([]);
            {
                var array124 = r.getVisibleRobots();
                for (var index123 = 0; index123 < array124.length; index123++) {
                    var robot = array124[index123];
                    {
                        if (unitType !== -1 && robot.unit !== unitType) {
                            continue;
                        }
                        if ((myTeam && (robot.team !== r.me.team)) || (!myTeam && (robot.team === r.me.team))) {
                            continue;
                        }
                        if (robot.x === r.me.x && robot.y === r.me.y) {
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
        Utils.getUnitsInRange = function (r, unitType, myTeam, minRadiusSq, maxRadiusSq) {
            var nearby = ([]);
            {
                var array126 = r.getVisibleRobots();
                for (var index125 = 0; index125 < array126.length; index125++) {
                    var robot = array126[index125];
                    {
                        if (unitType !== -1 && robot.unit !== unitType) {
                            continue;
                        }
                        if ((myTeam && (robot.team !== r.me.team)) || (!myTeam && (robot.team === r.me.team))) {
                            continue;
                        }
                        if (robot.x === r.me.x && robot.y === r.me.y) {
                            continue;
                        }
                        var distX = robot.x - r.me.x;
                        var distY = robot.y - r.me.y;
                        var distanceSquared = distX * distX + distY * distY;
                        if (distanceSquared >= minRadiusSq && distanceSquared <= maxRadiusSq) {
                            /* add */ (nearby.push(new bc19.Point(robot.x - r.me.x, robot.y - r.me.y)) > 0);
                        }
                    }
                }
            }
            return nearby;
        };
        Utils.getAdjacentFreeSpaces = function (r) {
            var freeSpaces = ([]);
            var dxes = [-1, 0, 1];
            var dyes = [-1, 0, 1];
            for (var index127 = 0; index127 < dxes.length; index127++) {
                var dx = dxes[index127];
                {
                    for (var index128 = 0; index128 < dyes.length; index128++) {
                        var dy = dyes[index128];
                        {
                            var delta = new bc19.Point(dx, dy);
                            if (Utils.isNearbySpaceEmpty(r, delta)) {
                                /* add */ (freeSpaces.push(delta) > 0);
                            }
                        }
                    }
                }
            }
            return freeSpaces;
        };
        Utils.getMirroredPosition = function (rob, position) {
            var passableMap = rob.getPassableMap();
            var ht = passableMap.length;
            var wid = passableMap[0].length;
            var locX = position.x;
            var locY = position.y;
            var verticalSymmetry = true;
            for (var c = 0; c < wid; c++) {
                for (var r = 0; r < (ht / 2 | 0) + 1; r++) {
                    if (passableMap[r][c] !== passableMap[ht - r - 1][c]) {
                        verticalSymmetry = false;
                        break;
                    }
                }
                ;
            }
            ;
            if (verticalSymmetry) {
                return new bc19.Point(locX, ht - locY - 1);
            }
            return new bc19.Point(wid - locX - 1, locY);
        };
        Utils.computeSquareDistance = function (p1, p2) {
            return (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y);
        };
        Utils.getLocation = function (r) {
            return new bc19.Point(r.x, r.y);
        };
        Utils.getRobotsInRange = function (r, unitType, myTeam, minRadiusSq, maxRadiusSq) {
            var nearby = ([]);
            {
                var array130 = r.getVisibleRobots();
                for (var index129 = 0; index129 < array130.length; index129++) {
                    var robot = array130[index129];
                    {
                        if (unitType !== -1 && robot.unit !== unitType) {
                            continue;
                        }
                        if ((myTeam && (robot.team !== r.me.team)) || (!myTeam && (robot.team === r.me.team))) {
                            continue;
                        }
                        if (robot.x === r.me.x && robot.y === r.me.y) {
                            continue;
                        }
                        var distX = robot.x - r.me.x;
                        var distY = robot.y - r.me.y;
                        var distanceSquared = distX * distX + distY * distY;
                        if (distanceSquared >= minRadiusSq && distanceSquared <= maxRadiusSq) {
                            /* add */ (nearby.push(robot) > 0);
                        }
                    }
                }
            }
            return nearby;
        };
        Utils.generateRingLocations = function (r, castle, enemyCastle) {
            var ringLocations = ({});
            var passableMap = r.getPassableMap();
            for (var y = 0; y < passableMap.length; y++) {
                for (var x = 0; x < passableMap[y].length; x++) {
                    if (!passableMap[y][x] || !Utils.isBetween(castle, enemyCastle, new bc19.Point(x, y))) {
                        continue;
                    }
                    var dx = x - castle.x;
                    var dy = y - castle.y;
                    var distance = (Math.sqrt(dx * dx + dy * dy) | 0);
                    if (!(function (m, k) { if (m.entries == null)
                        m.entries = []; for (var i = 0; i < m.entries.length; i++)
                        if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                            return true;
                        } return false; })(ringLocations, distance)) {
                        /* put */ (function (m, k, v) { if (m.entries == null)
                            m.entries = []; for (var i = 0; i < m.entries.length; i++)
                            if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                                m.entries[i].value = v;
                                return;
                            } m.entries.push({ key: k, value: v, getKey: function () { return this.key; }, getValue: function () { return this.value; } }); })(ringLocations, distance, []);
                    }
                    /* add */ ((function (m, k) { if (m.entries == null)
                        m.entries = []; for (var i = 0; i < m.entries.length; i++)
                        if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                            return m.entries[i].value;
                        } return null; })(ringLocations, distance).push(new bc19.Point(x, y)) > 0);
                }
                ;
            }
            ;
            return ringLocations;
        };
        Utils.getCastleLocation = function (r) {
            var initialCastleDelta = Utils.getAdjacentUnits(r, r.SPECS.CASTLE, true)[0];
            return new bc19.Point(r.me.x + initialCastleDelta.x, r.me.y + initialCastleDelta.y);
        };
        Utils.getRobotSortInRange = function (r, myTeam, minRadiusSq, maxRadiusSq) {
            var nearby = ([]);
            {
                var array132 = r.getVisibleRobots();
                for (var index131 = 0; index131 < array132.length; index131++) {
                    var robot = array132[index131];
                    {
                        if ((myTeam && (robot.team !== r.me.team)) || (!myTeam && (robot.team === r.me.team))) {
                            continue;
                        }
                        if (robot.x === r.me.x && robot.y === r.me.y) {
                            continue;
                        }
                        var distX = robot.x - r.me.x;
                        var distY = robot.y - r.me.y;
                        var distanceSquared = distX * distX + distY * distY;
                        if (distanceSquared >= minRadiusSq && distanceSquared <= maxRadiusSq) {
                            var rob = new bc19.RobotSort(robot.id, robot.unit, robot.x, robot.y, distanceSquared, robot.health);
                            /* add */ (nearby.push(rob) > 0);
                        }
                    }
                }
            }
            /* sort */ nearby.sort();
            return nearby;
        };
        Utils.getAdjacentRobots = function (r, unitType, myTeam) {
            var nearby = ([]);
            {
                var array134 = r.getVisibleRobots();
                for (var index133 = 0; index133 < array134.length; index133++) {
                    var robot = array134[index133];
                    {
                        if (unitType !== -1 && robot.unit !== unitType) {
                            continue;
                        }
                        if ((myTeam && (robot.team !== r.me.team)) || (!myTeam && (robot.team === r.me.team))) {
                            continue;
                        }
                        if (robot.x === r.me.x && robot.y === r.me.y) {
                            continue;
                        }
                        if (Math.abs(robot.x - r.me.x) <= 1 && Math.abs(robot.y - r.me.y) <= 1) {
                            /* add */ (nearby.push(robot) > 0);
                        }
                    }
                }
            }
            return nearby;
        };
        return Utils;
    }());
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
        function Pilgrim() {
        }
        Pilgrim.state_$LI$ = function () { if (Pilgrim.state == null)
            Pilgrim.state = Pilgrim.State.GATHERING; return Pilgrim.state; };
        ;
        Pilgrim.computeCastleMap = function (r) {
            var targets = ([]);
            {
                var array136 = r.getVisibleRobots();
                for (var index135 = 0; index135 < array136.length; index135++) {
                    var robot = array136[index135];
                    {
                        if (robot.unit === r.SPECS.CASTLE || robot.unit === r.SPECS.CHURCH) {
                            /* add */ (targets.push(new bc19.Point(robot.x, robot.y)) > 0);
                        }
                    }
                }
            }
            Pilgrim.castleMap = new bc19.Navigation(r, r.getPassableMap(), targets);
        };
        Pilgrim.computeTargetMap = function (r, target) {
            var targetList = ([]);
            /* add */ (targetList.push(target) > 0);
            if (target == null) {
                r.log("cannot get target from castle because castle is dead");
            }
            Pilgrim.targetMap = new bc19.Navigation(r, r.getPassableMap(), targetList);
        };
        Pilgrim.computeMaps = function (r, target) {
            Pilgrim.computeTargetMap(r, target);
            Pilgrim.computeCastleMap(r);
        };
        Pilgrim.act = function (r) {
            if (r.__turn === 1) {
                var adjacentCastles = bc19.Utils.getAdjacentRobots(r, r.SPECS.CASTLE, true);
                var target = bc19.CommunicationUtils.getPilgrimTargetInfo(r, /* get */ adjacentCastles[0]);
                Pilgrim.computeMaps(r, target);
                Pilgrim.state = Pilgrim.State.GATHERING;
                bc19.CommunicationUtils.sendPilgrimInfoToCastle(r, target, 5);
            }
            if (Pilgrim.state_$LI$() === Pilgrim.State.GATHERING) {
                if (Pilgrim.targetMap.getPotential(bc19.Utils.getLocation(r.me)) === 0) {
                    if (r.me.karbonite < bc19.Utils.mySpecs(r).KARBONITE_CAPACITY && r.me.fuel < bc19.Utils.mySpecs(r).FUEL_CAPACITY) {
                        if (bc19.Utils.canMine(r)) {
                            return r.mine();
                        }
                    }
                    else {
                        Pilgrim.state = Pilgrim.State.MOVING_RESOURCE_HOME;
                        return Pilgrim.act(r);
                    }
                }
                else {
                    return bc19.Utils.moveDijkstraThenRandom(r, Pilgrim.targetMap, 2);
                }
            }
            if (Pilgrim.state_$LI$() === Pilgrim.State.MOVING_RESOURCE_HOME) {
                var adjacentPlacesToDeposit = bc19.Utils.getAdjacentUnits(r, r.SPECS.CASTLE, true);
                /* addAll */ (function (l1, l2) { return l1.push.apply(l1, l2); })(adjacentPlacesToDeposit, bc19.Utils.getAdjacentUnits(r, r.SPECS.CHURCH, true));
                if (adjacentPlacesToDeposit.length > 0) {
                    if (r.me.karbonite > 0 || r.me.fuel > 0) {
                        var adjacentDeposit = adjacentPlacesToDeposit[0];
                        return r.give(adjacentDeposit.x, adjacentDeposit.y, r.me.karbonite, r.me.fuel);
                    }
                    else {
                        Pilgrim.state = Pilgrim.State.GATHERING;
                        return Pilgrim.act(r);
                    }
                }
                else {
                    return bc19.Utils.moveDijkstraThenRandom(r, Pilgrim.castleMap, 2);
                }
            }
            return null;
        };
        return Pilgrim;
    }());
    Pilgrim.targetMap = null;
    Pilgrim.castleMap = null;
    bc19.Pilgrim = Pilgrim;
    Pilgrim["__class"] = "bc19.Pilgrim";
    (function (Pilgrim) {
        var State;
        (function (State) {
            State[State["GATHERING"] = 0] = "GATHERING";
            State[State["MOVING_RESOURCE_HOME"] = 1] = "MOVING_RESOURCE_HOME";
        })(State = Pilgrim.State || (Pilgrim.State = {}));
    })(Pilgrim = bc19.Pilgrim || (bc19.Pilgrim = {}));
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Castle = (function () {
        function Castle() {
        }
        Castle.pilgrimToTarget_$LI$ = function () { if (Castle.pilgrimToTarget == null)
            Castle.pilgrimToTarget = ({}); return Castle.pilgrimToTarget; };
        ;
        Castle.targets_$LI$ = function () { if (Castle.targets == null)
            Castle.targets = ([]); return Castle.targets; };
        ;
        /*private*/ Castle.populateTargets = function (r) {
            var mySpot = ([]);
            /* add */ (mySpot.push(bc19.Utils.getLocation(r.me)) > 0);
            var myMap = new bc19.Navigation(r, r.getPassableMap(), mySpot);
            var karbPoints = Castle.computeKarbPoints(r);
            var fuelPoints = Castle.computeFuelPoints(r);
            while ((karbPoints.length > 0 || fuelPoints.length > 0)) {
                if (karbPoints.length > 0 && (fuelPoints.length === 0 || Castle.targets_$LI$().length % 2 === 0)) {
                    var bestIndex = 0;
                    for (var index = 1; index < karbPoints.length; index++) {
                        if (myMap.getPotential(/* get */ karbPoints[index]) < myMap.getPotential(/* get */ karbPoints[bestIndex])) {
                            bestIndex = index;
                        }
                    }
                    ;
                    /* add */ (Castle.targets_$LI$().push(/* get */ karbPoints[bestIndex]) > 0);
                    /* remove */ karbPoints.splice(bestIndex, 1);
                }
                else {
                    var bestIndex = 0;
                    for (var index = 1; index < fuelPoints.length; index++) {
                        if (myMap.getPotential(/* get */ fuelPoints[index]) < myMap.getPotential(/* get */ fuelPoints[bestIndex])) {
                            bestIndex = index;
                        }
                    }
                    ;
                    /* add */ (Castle.targets_$LI$().push(/* get */ fuelPoints[bestIndex]) > 0);
                    /* remove */ fuelPoints.splice(bestIndex, 1);
                }
            }
            ;
        };
        /*private*/ Castle.computeKarbPoints = function (r) {
            var karboniteMap = r.getKarboniteMap();
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
            return targets;
        };
        /*private*/ Castle.computeFuelPoints = function (r) {
            var fuelMaps = r.getFuelMap();
            var targets = ([]);
            for (var y = 0; y < fuelMaps.length; y++) {
                for (var x = 0; x < fuelMaps[y].length; x++) {
                    if (fuelMaps[y][x]) {
                        /* add */ (targets.push(new bc19.Point(x, y)) > 0);
                    }
                }
                ;
            }
            ;
            return targets;
        };
        Castle.act = function (r) {
            if (r.__turn === 1) {
                Castle.populateTargets(r);
            }
            var robots = bc19.Utils.getRobotsInRange(r, r.SPECS.PILGRIM, true, 0, 5);
            for (var index137 = 0; index137 < robots.length; index137++) {
                var rob = robots[index137];
                {
                    var target = bc19.CommunicationUtils.getPilgrimTargetForCastle(r, rob);
                    if (target != null) {
                        /* put */ (function (m, k, v) { if (m.entries == null)
                            m.entries = []; for (var i = 0; i < m.entries.length; i++)
                            if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                                m.entries[i].value = v;
                                return;
                            } m.entries.push({ key: k, value: v, getKey: function () { return this.key; }, getValue: function () { return this.value; } }); })(Castle.pilgrimToTarget_$LI$(), rob.id, target);
                    }
                }
            }
            var allRobots = ([]);
            {
                var array139 = r.getVisibleRobots();
                for (var index138 = 0; index138 < array139.length; index138++) {
                    var rob = array139[index138];
                    {
                        if ((function (m, k) { if (m.entries == null)
                            m.entries = []; for (var i = 0; i < m.entries.length; i++)
                            if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                                return true;
                            } return false; })(Castle.pilgrimToTarget_$LI$(), rob.id)) {
                            /* add */ (function (s, e) { if (s.indexOf(e) == -1) {
                                s.push(e);
                                return true;
                            }
                            else {
                                return false;
                            } })(allRobots, rob.id);
                        }
                    }
                }
            }
            {
                var array141 = (function (m) { var r = []; if (m.entries == null)
                    m.entries = []; for (var i = 0; i < m.entries.length; i++)
                    r.push(m.entries[i].key); return r; })(Castle.pilgrimToTarget_$LI$());
                for (var index140 = 0; index140 < array141.length; index140++) {
                    var id = array141[index140];
                    {
                        if (!(allRobots.indexOf((id)) >= 0)) {
                            /* add */ Castle.targets_$LI$().splice(0, 0, /* get */ (function (m, k) { if (m.entries == null)
                                m.entries = []; for (var i = 0; i < m.entries.length; i++)
                                if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                                    return m.entries[i].value;
                                } return null; })(Castle.pilgrimToTarget_$LI$(), id));
                            Castle.initialPilgrimsBuilt--;
                            /* remove */ (function (m, k) { if (m.entries == null)
                                m.entries = []; for (var i = 0; i < m.entries.length; i++)
                                if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                                    return m.entries.splice(i, 1)[0];
                                } })(Castle.pilgrimToTarget_$LI$(), id);
                        }
                    }
                }
            }
            if (Castle.initialPilgrimsBuilt < bc19.Constants.CASTLE_MAX_INITIAL_PILGRIMS) {
                bc19.CommunicationUtils.sendPilgrimInfo(r, /* get */ Castle.targets_$LI$()[0], 3);
                var action_1 = bc19.Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
                if (action_1 != null) {
                    Castle.initialPilgrimsBuilt++;
                    /* remove */ Castle.targets_$LI$().splice(0, 1);
                    return action_1;
                }
            }
            var numPilgrims = bc19.Utils.getUnitsInRange(r, r.SPECS.PILGRIM, true, 0, bc19.Utils.mySpecs(r).VISION_RADIUS).length;
            if (numPilgrims < 1) {
                var action_2 = bc19.Utils.tryAndBuildInRandomSpace(r, r.SPECS.PILGRIM);
                if (action_2 != null) {
                    return action_2;
                }
            }
            var action = bc19.Utils.tryAndBuildInRandomSpace(r, r.SPECS.PROPHET);
            if (action != null) {
                return action;
            }
            return null;
        };
        return Castle;
    }());
    Castle.initialPilgrimsBuilt = 0;
    Castle.numFuelWorkers = 0;
    Castle.numKarbWorkers = 0;
    bc19.Castle = Castle;
    Castle["__class"] = "bc19.Castle";
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
        /**
         *
         * @param {bc19.Point} point
         * @return {number}
         */
        Point.prototype.compareTo = function (point) {
            return 0;
        };
        Point.prototype.toString = function () {
            return "(" + this.x + ", " + this.y + ")";
        };
        return Point;
    }());
    bc19.Point = Point;
    Point["__class"] = "bc19.Point";
    Point["__interfaces"] = ["java.lang.Comparable"];
})(bc19 || (bc19 = {}));
(function (bc19) {
    /**
     * Created by patil215 on 1/18/19.
     * @class
     */
    var Preacher = (function () {
        function Preacher() {
        }
        Preacher.act = function (r) {
            return null;
        };
        return Preacher;
    }());
    bc19.Preacher = Preacher;
    Preacher["__class"] = "bc19.Preacher";
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
            if (this.fuel < Math.ceil(Math.sqrt(radius)))
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
            if (this.me.unit === this.SPECS.CHURCH)
                throw new bc19.BCException("Churches cannot attack.");
            if (this.fuel < this.SPECS.UNITS[this.me.unit].ATTACK_FUEL_COST)
                throw new bc19.BCException("Not enough fuel to attack.");
            if (!this.checkOnMap(this.me.x + dx, this.me.y + dy))
                throw new bc19.BCException("Can\'t attack off of map.");
            if (this.gameState.shadow[this.me.y + dy][this.me.x + dx] === -1)
                throw new bc19.BCException("Cannot attack outside of vision range.");
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
    /**
     * Created by patil215 on 1/18/19.
     * @class
     */
    var Constants = (function () {
        function Constants() {
        }
        return Constants;
    }());
    Constants.CASTLE_MAX_INITIAL_PILGRIMS = 2;
    Constants.PILGRIM_MINE_FUEL_COST = 1;
    bc19.Constants = Constants;
    Constants["__class"] = "bc19.Constants";
})(bc19 || (bc19 = {}));
(function (bc19) {
    var Church = (function () {
        function Church() {
        }
        Church.act = function (r) {
            return null;
        };
        return Church;
    }());
    bc19.Church = Church;
    Church["__class"] = "bc19.Church";
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
        function Prophet() {
        }
        Prophet.ringLocations_$LI$ = function () { if (Prophet.ringLocations == null)
            Prophet.ringLocations = ({}); return Prophet.ringLocations; };
        ;
        Prophet.ring_$LI$ = function () { if (Prophet.ring == null)
            Prophet.ring = Prophet.RING_START; return Prophet.ring; };
        ;
        Prophet.state_$LI$ = function () { if (Prophet.state == null)
            Prophet.state = Prophet.State.TURTLING; return Prophet.state; };
        ;
        Prophet.pickRingTarget = function (r) {
            var pointsInRing = (function (m, k) { if (m.entries == null)
                m.entries = []; for (var i = 0; i < m.entries.length; i++)
                if (m.entries[i].key.equals != null && m.entries[i].key.equals(k) || m.entries[i].key === k) {
                    return m.entries[i].value;
                } return null; })(Prophet.ringLocations_$LI$(), Prophet.ring_$LI$());
            if (Prophet.ring_$LI$() > Prophet.RING_START) {
                return bc19.Utils.findClosestPoint(r, pointsInRing);
            }
            else {
                return pointsInRing[((Math.random() * pointsInRing.length) | 0)];
            }
        };
        Prophet.receivedBumpSignal = function (r) {
            {
                var array143 = r.getVisibleRobots();
                for (var index142 = 0; index142 < array143.length; index142++) {
                    var robot = array143[index142];
                    {
                        if (robot.signal === r.id) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };
        Prophet.beginAttack = function (r) {
            if (bc19.Utils.canSignal(r, Prophet.ATTACK_SIGNAL_RADIUS_SQ)) {
                r.signal(Prophet.ATTACK_SIGNAL, Prophet.ATTACK_SIGNAL_RADIUS_SQ);
                Prophet.state = Prophet.State.ATTACKING;
                return Prophet.act(r);
            }
            return null;
        };
        Prophet.ringFormation = function (r) {
            if (Prophet.receivedBumpSignal(r) && bc19.Utils.isOn(r, Prophet.ringTarget)) {
                if (Prophet.ring_$LI$() >= Prophet.MAX_RING_LEVEL) {
                    return Prophet.beginAttack(r);
                }
                bc19.Prophet.ring_$LI$();
                Prophet.ring++;
                Prophet.ringTarget = null;
            }
            if (Prophet.ringTarget == null) {
                Prophet.ringTarget = Prophet.pickRingTarget(r);
                if (Prophet.ringTarget == null) {
                    return null;
                }
                var targets = ([]);
                /* add */ (targets.push(Prophet.ringTarget) > 0);
                Prophet.ringMap = new bc19.Navigation(r, r.getPassableMap(), targets);
            }
            var visibleMap = r.getVisibleRobotMap();
            if (bc19.Utils.isAdjacentOrOn(r, Prophet.ringTarget) && !bc19.Utils.isOn(r, Prophet.ringTarget) && visibleMap[Prophet.ringTarget.y][Prophet.ringTarget.x] > 0) {
                if (bc19.Utils.canSignal(r, 2)) {
                    var id = r.getVisibleRobotMap()[Prophet.ringTarget.y][Prophet.ringTarget.x];
                    r.signal(id, 2);
                }
                return null;
            }
            if (!bc19.Utils.isOn(r, Prophet.ringTarget) && Prophet.ringMap != null) {
                return bc19.Utils.moveDijkstra(r, Prophet.ringMap, 1);
            }
            return null;
        };
        Prophet.computeMaps = function (r) {
            var targets = ([]);
            /* add */ (targets.push(bc19.Utils.getMirroredPosition(r, new bc19.Point(r.me.x, r.me.y))) > 0);
            Prophet.enemyCastleMap = new bc19.Navigation(r, r.getPassableMap(), targets);
        };
        Prophet.shouldMoveTowardsCastles = function (r) {
            var numFriendliesNearby = bc19.Utils.getUnitsInRange(r, -1, true, 0, Number.MAX_VALUE).length;
            var probabilityMoving = 1.0 / (1.0 + Math.exp(-(numFriendliesNearby - 4)));
            return Math.random() < probabilityMoving;
        };
        Prophet.receivedAttackSignal = function (r) {
            {
                var array145 = r.getVisibleRobots();
                for (var index144 = 0; index144 < array145.length; index144++) {
                    var robot = array145[index144];
                    {
                        if (robot.signal === Prophet.ATTACK_SIGNAL) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };
        Prophet.doFirstTurnActions = function (r) {
            Prophet.computeMaps(r);
            Prophet.initialCastleLocation = bc19.Utils.getCastleLocation(r);
            Prophet.enemyCastleLocation = bc19.Utils.getMirroredPosition(r, Prophet.initialCastleLocation);
            Prophet.ringLocations = bc19.Utils.generateRingLocations(r, Prophet.initialCastleLocation, Prophet.enemyCastleLocation);
        };
        Prophet.act = function (r) {
            if (r.__turn === 1) {
                Prophet.doFirstTurnActions(r);
            }
            var attackAction = bc19.Utils.tryAndAttack(r, bc19.Utils.mySpecs(r).ATTACK_RADIUS[1]);
            if (attackAction != null) {
                return attackAction;
            }
            if (Prophet.state_$LI$() === Prophet.State.TURTLING) {
                if (Prophet.receivedAttackSignal(r)) {
                    return Prophet.beginAttack(r);
                }
                return Prophet.ringFormation(r);
            }
            else if (Prophet.state_$LI$() === Prophet.State.ATTACKING) {
                return bc19.Utils.moveDijkstraThenRandom(r, Prophet.enemyCastleMap, 1);
            }
            return null;
        };
        return Prophet;
    }());
    Prophet.ringTarget = null;
    Prophet.RING_START = 3;
    Prophet.MAX_RING_LEVEL = 8;
    Prophet.ATTACK_SIGNAL = 65532;
    Prophet.ATTACK_SIGNAL_RADIUS_SQ = 5;
    Prophet.ringMap = null;
    Prophet.enemyCastleMap = null;
    Prophet.initialCastleLocation = null;
    Prophet.enemyCastleLocation = null;
    bc19.Prophet = Prophet;
    Prophet["__class"] = "bc19.Prophet";
    (function (Prophet) {
        var State;
        (function (State) {
            State[State["TURTLING"] = 0] = "TURTLING";
            State[State["ATTACKING"] = 1] = "ATTACKING";
        })(State = Prophet.State || (Prophet.State = {}));
    })(Prophet = bc19.Prophet || (bc19.Prophet = {}));
})(bc19 || (bc19 = {}));
(function (bc19) {
    var RobotSort = (function () {
        function RobotSort(id_, unit_, x_, y_, dist_, hp_) {
            this.id = 0;
            this.unit = 0;
            this.x = 0;
            this.y = 0;
            this.dist = 0;
            this.hp = 0;
            this.id = id_;
            this.unit = unit_;
            this.x = x_;
            this.y = y_;
            this.dist = dist_;
            this.hp = hp_;
        }
        RobotSort.getPriority = function (type) {
            if (type === 5)
                return 0;
            if (type === 4)
                return 1;
            if (type === 3)
                return 2;
            if (type === 0)
                return 3;
            if (type === 1)
                return 4;
            if (type === 2)
                return 5;
            return -1;
        };
        /**
         *
         * @param {bc19.RobotSort} r
         * @return {number}
         */
        RobotSort.prototype.compareTo = function (r) {
            if (this.unit === r.unit) {
                if (this.hp === r.hp) {
                    return this.dist - r.dist;
                }
                else {
                    return this.hp - r.hp;
                }
            }
            else {
                return RobotSort.getPriority(this.unit) - RobotSort.getPriority(r.unit);
            }
        };
        return RobotSort;
    }());
    bc19.RobotSort = RobotSort;
    RobotSort["__class"] = "bc19.RobotSort";
    RobotSort["__interfaces"] = ["java.lang.Comparable"];
})(bc19 || (bc19 = {}));
(function (bc19) {
    var CommunicationUtils = (function () {
        function CommunicationUtils() {
        }
        CommunicationUtils.PILGRIM_TARGET_MASK_$LI$ = function () { if (CommunicationUtils.PILGRIM_TARGET_MASK == null)
            CommunicationUtils.PILGRIM_TARGET_MASK = ((7 << 13) | 0); return CommunicationUtils.PILGRIM_TARGET_MASK; };
        ;
        CommunicationUtils.CASTLE_INFORM_MASK_$LI$ = function () { if (CommunicationUtils.CASTLE_INFORM_MASK == null)
            CommunicationUtils.CASTLE_INFORM_MASK = ((6 << 13) | 0); return CommunicationUtils.CASTLE_INFORM_MASK; };
        ;
        /*private*/ CommunicationUtils.sendBroadcast = function (r, message, radius) {
            r.signal(message, radius);
        };
        CommunicationUtils.sendPilgrimInfo = function (r, target, range) {
            var message = ((CommunicationUtils.PILGRIM_TARGET_MASK_$LI$() + (target.x << 6) + target.y) | 0);
            CommunicationUtils.sendBroadcast(r, message, range);
        };
        CommunicationUtils.getPilgrimTargetInfo = function (r, rob) {
            if (r.isRadioing(rob) && (rob.signal >>> 12 === CommunicationUtils.PILGRIM_TARGET_MASK_$LI$() >>> 12)) {
                var message = (rob.signal | 0);
                return new bc19.Point(((message / (64) | 0)) % 64, message % 64);
            }
            r.log("failed to get target info. isRadioing: " + r.isRadioing(rob) + " found mask " + (((rob.signal >>> 12) | 0)) + " wanted mask " + (CommunicationUtils.PILGRIM_TARGET_MASK_$LI$() >>> 12));
            return null;
        };
        CommunicationUtils.sendPilgrimInfoToCastle = function (r, target, range) {
            var message = ((CommunicationUtils.CASTLE_INFORM_MASK_$LI$() + (target.x << 6) + target.y) | 0);
            CommunicationUtils.sendBroadcast(r, message, range);
        };
        CommunicationUtils.getPilgrimTargetForCastle = function (r, rob) {
            if (r.isRadioing(rob) && (rob.signal >>> 12 === CommunicationUtils.CASTLE_INFORM_MASK_$LI$() >>> 12)) {
                var message = (rob.signal | 0);
                return new bc19.Point(((message / (64) | 0)) % 64, message % 64);
            }
            return null;
        };
        return CommunicationUtils;
    }());
    bc19.CommunicationUtils = CommunicationUtils;
    CommunicationUtils["__class"] = "bc19.CommunicationUtils";
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
            for (var index146 = 0; index146 < dxes.length; index146++) {
                var dx = dxes[index146];
                {
                    for (var index147 = 0; index147 < dyes.length; index147++) {
                        var dy = dyes[index147];
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
            for (var index148 = 0; index148 < this.targets.length; index148++) {
                var target = this.targets[index148];
                {
                    this.distances[target.y][target.x] = 0;
                    queue.enqueue(new bc19.Point(target.x, target.y));
                }
            }
            while ((!queue.isEmpty())) {
                var loc = queue.dequeue();
                var curDistance = this.distances[loc.y][loc.x];
                for (var index149 = 0; index149 < movementDeltas.length; index149++) {
                    var disp = movementDeltas[index149];
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
         * Returns a best delta to move according to a start location and radius (not r_squared, just r)
         * <p>
         * Tries all possible directions, returning their optimality in sorted order.
         * <p>
         * TODO: Uses some sort of heuristic to weight moving quick against using fuel.
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
            for (var index150 = 0; index150 < possibleDeltas.length; index150++) {
                var delta = possibleDeltas[index150];
                {
                    var newX = start.x + delta.x;
                    var newY = start.y + delta.y;
                    if (bc19.Utils.canMove(this.r, delta) && this.distances[newY][newX] < minDist) {
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
        Navigation.prototype.getPotential = function (target) {
            return this.distances[target.y][target.x];
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
            this.doUnitPreTurnActions();
            var actionToDo = null;
            if (this.me.unit === this.SPECS.CASTLE) {
                actionToDo = bc19.Castle.act(this);
            }
            else if (this.me.unit === this.SPECS.PILGRIM) {
                actionToDo = bc19.Pilgrim.act(this);
            }
            else if (this.me.unit === this.SPECS.CHURCH) {
                actionToDo = bc19.Church.act(this);
            }
            else if (this.me.unit === this.SPECS.CRUSADER) {
                actionToDo = bc19.Crusader.act(this);
            }
            else if (this.me.unit === this.SPECS.PROPHET) {
                actionToDo = bc19.Prophet.act(this);
            }
            else if (this.me.unit === this.SPECS.PREACHER) {
                actionToDo = bc19.Preacher.act(this);
            }
            this.doUnitPostTurnActions();
            return actionToDo;
        };
        /*private*/ MyRobot.prototype.doUnitPreTurnActions = function () {
            this.__turn++;
        };
        /*private*/ MyRobot.prototype.doUnitPostTurnActions = function () {
        };
        return MyRobot;
    }(bc19.BCAbstractRobot));
    bc19.MyRobot = MyRobot;
    MyRobot["__class"] = "bc19.MyRobot";
})(bc19 || (bc19 = {}));
bc19.CommunicationUtils.CASTLE_INFORM_MASK_$LI$();
bc19.CommunicationUtils.PILGRIM_TARGET_MASK_$LI$();
bc19.Prophet.state_$LI$();
bc19.Prophet.ring_$LI$();
bc19.Prophet.ringLocations_$LI$();
bc19.Castle.targets_$LI$();
bc19.Castle.pilgrimToTarget_$LI$();
bc19.Pilgrim.state_$LI$();
//# sourceMappingURL=bundle.js.map
var specs = {"COMMUNICATION_BITS":16,"CASTLE_TALK_BITS":8,"MAX_ROUNDS":1000,"TRICKLE_FUEL":25,"INITIAL_KARBONITE":100,"INITIAL_FUEL":500,"MINE_FUEL_COST":1,"KARBONITE_YIELD":2,"FUEL_YIELD":10,"MAX_TRADE":1024,"MAX_BOARD_SIZE":64,"MAX_ID":4096,"CASTLE":0,"CHURCH":1,"PILGRIM":2,"CRUSADER":3,"PROPHET":4,"PREACHER":5,"RED":0,"BLUE":1,"CHESS_INITIAL":100,"CHESS_EXTRA":20,"TURN_MAX_TIME":200,"MAX_MEMORY":50000000,"UNITS":[{"CONSTRUCTION_KARBONITE":null,"CONSTRUCTION_FUEL":null,"KARBONITE_CAPACITY":null,"FUEL_CAPACITY":null,"SPEED":0,"FUEL_PER_MOVE":null,"STARTING_HP":200,"VISION_RADIUS":100,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[1,64],"ATTACK_FUEL_COST":10,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":50,"CONSTRUCTION_FUEL":200,"KARBONITE_CAPACITY":null,"FUEL_CAPACITY":null,"SPEED":0,"FUEL_PER_MOVE":null,"STARTING_HP":100,"VISION_RADIUS":100,"ATTACK_DAMAGE":0,"ATTACK_RADIUS":0,"ATTACK_FUEL_COST":0,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":10,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":1,"STARTING_HP":10,"VISION_RADIUS":100,"ATTACK_DAMAGE":null,"ATTACK_RADIUS":null,"ATTACK_FUEL_COST":null,"DAMAGE_SPREAD":null},{"CONSTRUCTION_KARBONITE":15,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":9,"FUEL_PER_MOVE":1,"STARTING_HP":40,"VISION_RADIUS":49,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[1,16],"ATTACK_FUEL_COST":10,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":25,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":2,"STARTING_HP":20,"VISION_RADIUS":64,"ATTACK_DAMAGE":10,"ATTACK_RADIUS":[16,64],"ATTACK_FUEL_COST":25,"DAMAGE_SPREAD":0},{"CONSTRUCTION_KARBONITE":30,"CONSTRUCTION_FUEL":50,"KARBONITE_CAPACITY":20,"FUEL_CAPACITY":100,"SPEED":4,"FUEL_PER_MOVE":3,"STARTING_HP":60,"VISION_RADIUS":16,"ATTACK_DAMAGE":20,"ATTACK_RADIUS":[1,16],"ATTACK_FUEL_COST":15,"DAMAGE_SPREAD":3}]};
var robot = new bc19.MyRobot(); robot.setSpecs(specs);