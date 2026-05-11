# OS Project Demo Guide — F10 & F11 Intersection Simulation

---

## HOW TO USE THIS GUIDE
Each section = one rubric category. For each, you'll find:
- What it does (plain English)
- Where in the code (exact function/line to point at)
- What to say if teacher asks "what happens if you do X"

---

## RUBRIC 1 — System Architecture (40 pts)
### "Multi-Process Design: fork(), bidirectional pipes for emergency coordination"

### What's happening
Your program runs as **3 processes total**:
- **Parent process** (main) — runs the simulation + graphics
- **Child 1** — F10 controller (`controller_process(INTER_F10, ...)`)
- **Child 2** — F11 controller (`controller_process(INTER_F11, ...)`)

The children are created using `fork()`. They communicate with the parent and with each other using **pipes**.

### Where to point in code

**Pipe creation** (6 pipe pairs, all created before fork):
```c
pipe(g_pipe_to_f10)    // parent writes → F10 controller reads
pipe(g_pipe_from_f10)  // F10 controller writes → parent reads
pipe(g_pipe_to_f11)
pipe(g_pipe_from_f11)
pipe(g_pipe_c2c_a)     // F10 controller → F11 controller
pipe(g_pipe_c2c_b)     // F11 controller → F10 controller
```

**Fork for F10 controller:**
```c
g_pid_f10 = fork();
if (g_pid_f10 == 0) {
    // child: close unused pipe ends, then run controller
    controller_process(INTER_F10, ...);
    _exit(0);
}
```
Same pattern repeated for F11.

**Why close unused pipe ends?**
After fork, each process has a copy of ALL pipe file descriptors. You must close the ones you won't use. If you don't close the write end of a pipe in the reader, it will never get EOF — the reader blocks forever. This is a classic pipe bug.

**The controller_process() function** (child loop):
- Uses `select()` to watch two pipes: one from parent, one from the other controller
- Sends a heartbeat to parent every 400ms
- When it gets `MSG_EMERGENCY_ALERT` from parent, it **relays it to the other controller** via `g_pipe_c2c_a` or `g_pipe_c2c_b`

**IPC message structure:**
```c
typedef struct {
    int type;         // MSG_EMERGENCY_ALERT, MSG_EMERGENCY_CLEAR, MSG_HEARTBEAT
    int vehicle_id;
    int vehicle_type;
    int from_inter;
} IPCMsg;
```

**How emergency coordination works:**
1. Ambulance starts crossing F10 (going straight toward F11)
2. Parent calls `ipc_emergency_alert(INTER_F10, vid, vtype)` → sends to F10 controller
3. F10 controller relays it to F11 controller via c2c pipe
4. F11 controller prints "PREPARING intersection for incoming emergency"
5. When ambulance finishes, `ipc_emergency_done()` sends `MSG_EMERGENCY_CLEAR` and same relay happens

---

### "What if" questions for this section

**Q: What happens if you don't close unused pipe ends in the child?**
A: The child would have a copy of the write end of pipes it's supposed to read from. When the parent closes its write end, the child still has one open — so `read()` never returns 0 (EOF), and the child loops forever waiting for data that will never come.

**Q: What happens if a pipe is full and you keep writing?**
A: `write()` blocks until the reader reads some data and frees space. Pipe buffers are typically 64KB on Linux. In our code, messages are only 16 bytes, so this is not an issue.

**Q: Why use `select()` in the controller instead of just `read()`?**
A: The controller needs to watch TWO pipes at once (from parent AND from other controller). If you called `read()` on one, it would block and you'd miss messages on the other. `select()` lets you wait on multiple file descriptors simultaneously.

**Q: What happens if a child process crashes?**
A: The parent calls `waitpid()` during shutdown to reap zombie processes. Also, `signal(SIGPIPE, SIG_IGN)` prevents the parent from dying if it tries to write to a pipe after the child has exited.

---

## RUBRIC 2 — Concurrency & Threads (30 pts)
### "15 vehicle pthreads, proper metadata storage"

### What's happening
Each of the 15 vehicles is an OS thread. All 15 run **concurrently** in the same process (parent). They share memory (the `g_vehicles[]` array, intersection state, etc.) and coordinate using mutexes and condition variables.

### Where to point in code

**Vehicle struct (metadata):**
```c
typedef struct {
    int id;           // unique ID 0-14
    int type;         // VT_AMBULANCE, VT_BUS, VT_CAR, etc.
    int origin;       // which intersection it started at
    int axis;         // 1 = North-South, 0 = East-West
    int destination;  // DIR_STRAIGHT, DIR_LEFT, DIR_RIGHT
    int priority;     // 0=Emergency, 1=Bus, 2=Others
    long arrival_time;
    int state;        // VS_WAITING, VS_CROSSING, VS_PARKED, etc.
    int wants_parking;
    int inter_target;
} Vehicle;
```

**Thread creation:**
```c
pthread_t vth[TOTAL_VEHICLES];
for (i = 0; i < TOTAL_VEHICLES; i++)
    pthread_create(&vth[i], NULL, vehicle_thread, &g_vehicles[i]);
```
Each thread gets a pointer to its `Vehicle` struct as its argument.

**Thread lifecycle (vehicle_thread function):**
```
Sleep a random amount (stagger arrivals)
↓
Loop forever while g_running:
  1. Set state = VS_WAITING, record arrival_time
  2. If wants_parking → try to park (dual semaphore)
  3. Cross first intersection (intersection_cross)
  4. If destination == STRAIGHT → travel to other intersection, cross it too
  5. Set state = VS_DONE, increment g_trips_done
  6. Sleep 4-8 seconds cooldown
  7. Call reinit_vehicle() → randomise new trip, go back to step 1
```

**Type pool (ensures realistic distribution):**
```c
int type_pool[15] = {
    VT_AMBULANCE, VT_AMBULANCE,
    VT_FIRETRUCK, VT_FIRETRUCK,
    VT_BUS, VT_BUS,
    VT_CAR, VT_CAR, VT_CAR, VT_CAR,
    VT_BIKE, VT_BIKE, VT_BIKE,
    VT_TRACTOR, VT_TRACTOR
};
// Then Fisher-Yates shuffle so types are randomly assigned to IDs
```

**Two extra threads** (not vehicles):
- `light_cycle_thread` × 2 — one per intersection, cycles traffic lights
- `ipc_relay_thread` × 1 — parent side, reads messages from controller children

**Thread join at shutdown:**
```c
for (i = 0; i < TOTAL_VEHICLES; i++)
    pthread_join(vth[i], NULL);
pthread_join(light_th[0], NULL);
pthread_join(light_th[1], NULL);
pthread_join(ipc_th, NULL);
```

---

### "What if" questions for this section

**Q: Why do threads need mutexes if they have separate Vehicle structs?**
A: The Vehicle structs themselves are separate. But shared globals like `g_trips_done`, `g_log[]`, `g_emrg_count`, and the `Intersection` structs are accessed by all threads. Without mutexes, two threads could read-modify-write `g_trips_done` simultaneously and lose an increment (race condition).

**Q: What happens if you create too many threads?**
A: Each thread needs a stack (default ~8MB on Linux). 15 threads = 120MB just for stacks. In practice Linux can handle thousands of threads, but it's still good practice to limit them. Our 15 + 5 system threads is very reasonable.

**Q: Why does vehicle_thread loop forever instead of exiting?**
A: The requirement says 15 *persistent* vehicle threads. A thread that exits is gone — you'd need to create a new one. Looping is simpler and keeps the thread pool constant. Threads only exit when `g_running` becomes 0 (SIGINT / ESC key).

**Q: What is `reinit_vehicle()`?**
A: It resets a vehicle's state for its next trip — new random intersection target, axis, destination, parking preference. It does NOT change the vehicle's ID or type. The visual position is also reset to the road arm approach point.

---

## RUBRIC 3 — Parking Logic (40 pts)
### "Dual semaphores: 10 spots + bounded queue. Vehicles never block intersection while waiting."

### What's happening
Each intersection has a `ParkingLot` with two semaphores:
- `spots` semaphore — initialized to 10. Represents physical parking spots.
- `wait_queue` semaphore — initialized to 5 (`PARKING_QUEUE_MAX`). Represents waiting queue slots.

The key insight: a vehicle that wants to park must first claim a **queue slot**, then wait for a **spot**. While waiting for a spot, it is NOT at the intersection — so it never blocks traffic.

### Where to point in code

**ParkingLot struct:**
```c
typedef struct {
    sem_t  spots;       // init = 10 (PARKING_SPOTS)
    sem_t  wait_queue;  // init = 5  (PARKING_QUEUE_MAX)
    int    occupancy;
    pthread_mutex_t lock;
} ParkingLot;
```

**parking_init:**
```c
sem_init(&p->spots,      0, PARKING_SPOTS);    // 10
sem_init(&p->wait_queue, 0, PARKING_QUEUE_MAX); // 5
```
The `0` in the second argument means the semaphore is shared between threads (not processes).

**parking_enter() — the protocol:**
```c
// Step 1: Try to claim a waiting-queue slot (non-blocking)
if (sem_trywait(&p->wait_queue) != 0) {
    // Queue is full (5 vehicles already waiting) — give up, go cross instead
    return 0;
}
// Step 2: Block here (NOT at intersection) until a spot is free
sem_wait(&p->spots);
// Step 3: Release queue slot so another vehicle can queue
sem_post(&p->wait_queue);
// Now actually parked — increment occupancy
p->occupancy++;
return 1;
```

**parking_leave():**
```c
p->occupancy--;
sem_post(&p->spots);  // release the physical spot → unblocks next waiting vehicle
```

**In vehicle_thread — parking happens BEFORE crossing:**
```c
// STEP 1: Optional parking before crossing
if (v->wants_parking) {
    v->state = VS_PARKING;
    int parked = parking_enter(lot, v->id, v->inter_target);
    if (parked) {
        v->state = VS_PARKED;
        ms_sleep(rand_range(8000, 15000));  // stay parked 8-15 seconds
        parking_leave(lot, v->id, v->inter_target);
    }
}
// STEP 2: Cross intersection (only after parking is done)
intersection_cross(v);
```

**Why this design never blocks the intersection:**
- `sem_wait(&p->spots)` is in `parking_enter()`, called BEFORE `intersection_cross()`
- The vehicle blocks waiting for a spot in the parking lot, not inside the intersection logic
- It only proceeds to `intersection_cross()` after parking is complete

---

### "What if" questions for this section

**Q: What happens if all 10 spots are taken AND 5 vehicles are already in the queue?**
A: `sem_trywait(&p->wait_queue)` fails immediately (returns -1, errno = EAGAIN). The vehicle gives up on parking and goes straight to crossing instead. No blocking.

**Q: What if you used only one semaphore (just `spots`)?**
A: Then potentially unlimited vehicles could pile up calling `sem_wait(&p->spots)`. With 15 vehicle threads, up to 15 could be blocked waiting for spots, but they're still blocking in a thread (using stack memory and a kernel wait queue). The bounded queue semaphore limits this to 5, making the system more predictable.

**Q: What is `sem_trywait` vs `sem_wait`?**
A: `sem_wait` blocks until the semaphore > 0. `sem_trywait` returns immediately — if semaphore > 0 it decrements and returns 0 (success); if semaphore is 0 it returns -1 (fail). We use `sem_trywait` for `wait_queue` so we can decide immediately whether to queue or skip parking.

**Q: Why is `occupancy` protected by a mutex even though semaphores are used?**
A: Semaphores control access to the parking spots. The `occupancy` integer is a shared counter used for display. If two threads both do `p->occupancy++` without a mutex, they can overwrite each other (non-atomic operation on most architectures).

**Q: What happens during shutdown if a thread is blocked in `sem_wait(&p->spots)`?**
A: In the shutdown code, we call `sem_post(&p->spots)` twice per lot to unblock any waiting threads. The threads then check `g_running == 0` and exit their loop.

---

## RUBRIC 4 — Traffic Control (30 pts)
### "Priority: Emergency > Bus > Others. Non-conflicting movement logic."

### What's happening
All traffic logic lives in `intersection_cross()`. It handles two completely separate paths: emergency vehicles (preempt everything) and normal vehicles (obey a 4-phase light + priority rules).

### Where to point in code

**Priority mapping:**
```c
int prio_map[VT_COUNT] = { 0, 0, 1, 2, 2, 2 };
//  Ambulance=0, Firetruck=0, Bus=1, Car=2, Bike=2, Tractor=2
// 0 = highest (emergency), 1 = medium (bus), 2 = normal
```

**EMERGENCY PATH (absolute preemption):**
```c
if (is_emrg) {
    pthread_mutex_lock(&inter->cross_lock);
    inter->emergency_mode = 1;  // signal to all other threads
    
    // Wait for intersection to clear (any currently crossing vehicles finish)
    while (inter->vehicles_crossing > 0 && g_running)
        pthread_cond_wait(&inter->cross_cond, &inter->cross_lock);
    
    inter->vehicles_crossing++;
    // ...cross, then:
    inter->emergency_mode = 0;
    pthread_cond_broadcast(&inter->cross_cond);  // wake everyone
}
```
Key point: emergency doesn't just run a red light — it waits for the intersection to be physically clear first (no mid-intersection collisions), then preempts.

**NORMAL PATH — the 4-rule check:**
```c
while (g_running) {
    int can_cross = 1;
    
    // Rule 1: Emergency takes absolute priority
    if (inter->emergency_mode)
        can_cross = 0;
    
    // Rule 2: Capacity limit (max 3 vehicles crossing simultaneously)
    else if (inter->vehicles_crossing >= MAX_CROSS_CONCURRENT)
        can_cross = 0;
    
    // Rule 3: Traffic light — must match your axis
    else if (is_ns  && !light_allows_ns(phase))   can_cross = 0;
    else if (!is_ns && !light_allows_ew(phase))   can_cross = 0;
    
    // Rule 4: Bus priority — normal vehicles yield to waiting buses
    else if (!is_bus && is_ns  && inter->waiting_bus_ns > 0)   can_cross = 0;
    else if (!is_bus && !is_ns && inter->waiting_bus_ew > 0)   can_cross = 0;
    
    if (can_cross) break;
    pthread_cond_wait(&inter->cross_cond, &inter->cross_lock);
}
```

**Traffic light phases (light_cycle_thread):**
```
NS_GREEN (10s) → NS_YELLOW (3s) → EW_GREEN (10s) → EW_YELLOW (3s) → repeat
```
Each phase change: lock the mutex, update `light_phase`, broadcast to wake waiting vehicles.

**Non-conflicting movement:**
`MAX_CROSS_CONCURRENT = 3` — up to 3 vehicles can cross simultaneously. This simulates real intersections where non-conflicting movements (e.g. right-turn + through-traffic on green) can proceed together. Without this, it would be completely serialized (one at a time).

**Crossing times (realistic differences by type):**
```c
case VT_TRACTOR: cross_ms = rand_range(5000, 7000);  // slow
case VT_BUS:     cross_ms = rand_range(4000, 6000);  // medium-slow
case VT_BIKE:    cross_ms = rand_range(2000, 3000);  // fast
default:         cross_ms = rand_range(3000, 5000);  // cars
```

---

### "What if" questions for this section

**Q: What happens if an emergency vehicle arrives while others are crossing?**
A: `emergency_mode` is set to 1. `intersection_cross()` then does `while (inter->vehicles_crossing > 0) pthread_cond_wait(...)`. The emergency vehicle waits until those vehicles finish their current crossing. New vehicles trying to enter see `emergency_mode == 1` and are blocked. Once the intersection clears, the emergency vehicle enters and crosses. This prevents collision.

**Q: What if two emergency vehicles arrive simultaneously?**
A: Both set `emergency_mode = 1` and both wait on the condition variable. The first one to get `vehicles_crossing == 0` proceeds. The second waits because `vehicles_crossing` immediately becomes 1 again. They cross sequentially. No deadlock because each one sets `emergency_mode = 0` and broadcasts when done.

**Q: What does `pthread_cond_broadcast` do vs `pthread_cond_signal`?**
A: `signal` wakes ONE waiting thread. `broadcast` wakes ALL waiting threads. We use `broadcast` because multiple vehicles might be waiting and after an emergency clears, ALL of them need to re-evaluate whether they can cross now. Using `signal` would only wake one, and the others would keep sleeping even though they could now proceed.

**Q: Why does the light cycle still run during emergency mode?**
A: The light cycle thread runs independently. But inside `intersection_cross()`, the emergency path ignores the light phase entirely — it doesn't check `light_allows_ns()` or `light_allows_ew()`. Normal vehicles check `emergency_mode` first and won't enter regardless of what the light says.

**Q: Can two vehicles going opposite directions both cross on NS_GREEN at the same time?**
A: Yes! If V1 is going north and V2 is going south, both are on N-S axis, both see NS_GREEN. Both can proceed as long as `vehicles_crossing < 3`. This is intentional — opposite direction flows don't conflict.

---

## RUBRIC 5 — Stability & Cleanup (20 pts)
### "SIGINT caught, threads joined, all IPC/semaphore resources freed"

### What's happening
When you press Ctrl+C or ESC:
1. `g_running` is set to 0
2. All blocked threads are woken up
3. Every thread checks `g_running` in its loop and exits cleanly
4. Main joins all threads (waits for them to finish)
5. All resources are destroyed in the correct order

### Where to point in code

**Signal handler registration:**
```c
struct sigaction sa;
sa.sa_handler = sigint_handler;
sigemptyset(&sa.sa_mask);
sigaction(SIGINT, &sa, NULL);
sigaction(SIGTERM, &sa, NULL);
signal(SIGPIPE, SIG_IGN);  // ignore broken pipe (child died)
```

**Signal handler itself:**
```c
static void sigint_handler(int sig) {
    g_running = 0;
    // Only async-signal-safe operations here (write, not printf)
    write(STDOUT_FILENO, "\n[SIM] SIGINT received...\n", 25);
}
```
`g_running` is `volatile sig_atomic_t` — guaranteed safe to write from a signal handler.

**Shutdown sequence (in order):**
```c
g_running = 0;

// 1. Wake all threads blocked on condition variables
for (i = 0; i < 2; i++) {
    pthread_mutex_lock(&g_inter[i].cross_lock);
    pthread_cond_broadcast(&g_inter[i].cross_cond);
    pthread_mutex_unlock(&g_inter[i].cross_lock);
    
    // 2. Unblock threads blocked on semaphores
    sem_post(&g_inter[i].parking.spots);
    sem_post(&g_inter[i].parking.spots);      // post twice for safety
    sem_post(&g_inter[i].parking.wait_queue);
    sem_post(&g_inter[i].parking.wait_queue);
}

// 3. Join all vehicle threads
for (i = 0; i < TOTAL_VEHICLES; i++)
    pthread_join(vth[i], NULL);

// 4. Join light + IPC threads
pthread_join(light_th[0], NULL);
pthread_join(light_th[1], NULL);
pthread_join(ipc_th, NULL);

// 5. Destroy semaphores and mutexes
for (i = 0; i < 2; i++) {
    parking_destroy(&g_inter[i].parking);  // sem_destroy x2, mutex_destroy
    pthread_mutex_destroy(&g_inter[i].cross_lock);
    pthread_cond_destroy(&g_inter[i].cross_cond);
}

// 6. Close pipe file descriptors
close(g_pipe_to_f10[WRITE_END]);
close(g_pipe_from_f10[READ_END]);
// ... etc

// 7. Kill and wait for child processes
kill(g_pid_f10, SIGTERM);
waitpid(g_pid_f10, NULL, 0);
kill(g_pid_f11, SIGTERM);
waitpid(g_pid_f11, NULL, 0);

// 8. Destroy remaining mutexes
pthread_mutex_destroy(&g_done_lock);
pthread_mutex_destroy(&g_log_lock);
pthread_mutex_destroy(&g_emrg_lock);
```

---

### "What if" questions for this section

**Q: What happens if you don't call `pthread_join`?**
A: The threads become "detached" after the process exits, but more importantly, if main() returns while threads are still running, those threads are killed abruptly. This can leave shared resources (files, semaphores) in a corrupted state. `join` ensures clean exit.

**Q: What happens if you don't destroy semaphores?**
A: Named semaphores (sem_open) persist in the kernel even after the process exits. Unnamed semaphores (sem_init, like ours) are destroyed automatically when the process exits, but calling `sem_destroy` is good practice — it signals intent and helps detect bugs (e.g., destroying a semaphore while threads are still waiting on it causes undefined behavior — which is why we unblock threads first).

**Q: Why `volatile sig_atomic_t` for `g_running`?**
A: `volatile` tells the compiler not to cache this variable in a register — always read it from memory. `sig_atomic_t` guarantees the read/write is atomic (uninterruptible) on this architecture. Without `volatile`, an optimizing compiler might keep `g_running` in a register inside a loop, never see it change, and loop forever.

**Q: Why post the semaphores twice during shutdown?**
A: We don't know exactly how many threads are blocked. Posting twice ensures at least two threads can be unblocked. Those threads will check `g_running == 0` and exit, which is the goal. Posting more than the semaphore's original value is safe — it just means no thread was waiting, and future `sem_wait` calls will succeed immediately (but all threads are exiting anyway).

**Q: What if the child process is already dead when you call `kill()`?**
A: `kill()` returns -1 with errno = ESRCH (no such process). The code doesn't check this return value, which is fine — we still call `waitpid()` which will return immediately for an already-dead process. In production code you'd want to handle this, but for a demo it's acceptable.

---

## QUICK REFERENCE — Key Concepts

| Concept | Where | One-line explanation |
|---|---|---|
| fork() | main(), before first thread | Creates child processes for controllers |
| pipe() | main(), 6 pairs created | Unidirectional byte channels between processes |
| select() | controller_process() | Wait on multiple file descriptors simultaneously |
| pthread_create | main() | Creates concurrent threads for vehicles + lights |
| pthread_mutex | intersection_cross() | Protects shared intersection state from races |
| pthread_cond | intersection_cross() | Lets threads sleep and be woken when conditions change |
| sem_init | parking_init() | Creates counting semaphores for spots and queue |
| sem_trywait | parking_enter() | Non-blocking semaphore decrement (fail-fast) |
| sem_wait | parking_enter() | Blocking semaphore decrement (wait for resource) |
| sem_post | parking_leave() | Releases semaphore, wakes a waiting thread |
| SIGINT | sigint_handler() | Triggered by Ctrl+C, sets g_running=0 |
| sigaction | main() | Registers signal handler with SA_RESTART semantics |
| waitpid | shutdown section | Reaps child processes, prevents zombies |

---

## IF YOU GET STUCK IN THE DEMO

- **"How does X communicate with Y?"** → Always mention: the pipe file descriptors, the IPCMsg struct, and which pipe direction (read/write end).
- **"Prove threads are concurrent"** → Point to the log panel on screen — multiple vehicles are crossing/waiting simultaneously with different IDs.
- **"Where is the race condition prevented?"** → `pthread_mutex_lock(&inter->cross_lock)` before reading/writing `vehicles_crossing`, `light_phase`, etc.
- **"How do you know parking works correctly?"** → The parking lot visual shows occupancy. The log shows "in parking wait-queue", "PARKED", "LEFT parking" messages in order. Occupancy never exceeds 10.

---

*Good luck on your demo!*
