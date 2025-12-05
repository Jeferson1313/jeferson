import tkinter as tk
from tkinter import messagebox, filedialog
import threading
import time
import pyautogui
from pynput import mouse, keyboard
import json
import os

# ==========================
# VARIÁVEIS
# ==========================
gravando = False
executando = False
passos = []
keys_pressed = set()
active_mods = set()
janela = None
gravando_indicator = None
animar_gravacao = True

SPECIAL_KEYS = {
    "Key.enter": "enter",
    "Key.space": "space",
    "Key.backspace": "backspace",
    "Key.tab": "tab",
    "Key.esc": "esc",
    "Key.delete": "delete",
    "Key.shift": "shift",
    "Key.shift_l": "shift",
    "Key.shift_r": "shift",
    "Key.ctrl": "ctrl",
    "Key.ctrl_l": "ctrl",
    "Key.ctrl_r": "ctrl",
    "Key.alt": "alt",
    "Key.alt_l": "alt",
    "Key.alt_r": "alt",
    "Key.cmd": "win",
    "Key.cmd_l": "win",
    "Key.cmd_r": "win",
    "Key.up": "up",
    "Key.down": "down",
    "Key.left": "left",
    "Key.right": "right",
}

listener_mouse = None
listener_keyboard = None
ultimo_clique = {"tempo": 0, "button": None}

# ==========================
# MODIFIERS
# ==========================
MODIFIER_KEYS = [
    ("ctrl",  [keyboard.Key.ctrl, keyboard.Key.ctrl_l, keyboard.Key.ctrl_r]),
    ("shift", [keyboard.Key.shift, keyboard.Key.shift_l, keyboard.Key.shift_r]),
    ("alt",   [keyboard.Key.alt, keyboard.Key.alt_l, keyboard.Key.alt_r]),
    ("cmd",   [keyboard.Key.cmd, keyboard.Key.cmd_l, keyboard.Key.cmd_r]),
]

def is_modifier(key):
    for name, keys in MODIFIER_KEYS:
        if key in keys:
            return True, name
    return False, None

# ==========================
# GRAVAÇÃO
# ==========================
def registrar_tecla_simples(key):
    try:
        try:
            valor = key.char
            if valor is None:
                valor = str(key)
        except:
            valor = str(key)
    except Exception:
        valor = str(key)
    passos.append(("key_press", valor))
    atualizar_lista()

def registrar_hotkey(mods, key):
    try:
        tecla = key.char
    except Exception:
        tecla = str(key)
    combo = list(mods) + [tecla]
    passos.append(("hotkey", combo))
    atualizar_lista()

def registrar_clique(x, y, button):
    passos.append(("mouse_click", x, y, str(button)))
    atualizar_lista()

# ==========================
# LISTENERS
# ==========================
def on_click(x, y, button, pressed):
    global ultimo_clique

    if gravando and pressed:
        agora = time.time()

        if button == ultimo_clique["button"] and (agora - ultimo_clique["tempo"]) < 0.30:
            passos.append(("double_click", x, y, str(button)))
            atualizar_lista()
        else:
            registrar_clique(x, y, button)

        ultimo_clique = {"tempo": agora, "button": button}

def on_scroll(x, y, dx, dy):
    if gravando:
        passos.append(("scroll", x, y, dx, dy))
        atualizar_lista()

def on_key_press(key):
    global gravando

    if gravando:
        try:
            if isinstance(key, keyboard.KeyCode) and key.char and key.char.lower() == "s":
                if "ctrl" in active_mods and "shift" in active_mods:
                    parar_gravacao()
                    return
        except Exception:
            pass

    if key in keys_pressed:
        return
    keys_pressed.add(key)

    mod, nome = is_modifier(key)
    if mod:
        active_mods.add(nome)
        return

    if not gravando:
        return

    if active_mods:
        registrar_hotkey(active_mods.copy(), key)
    else:
        registrar_tecla_simples(key)

def on_key_release(key):
    if key in keys_pressed:
        keys_pressed.remove(key)

    mod, nome = is_modifier(key)
    if mod and nome in active_mods:
        active_mods.remove(nome)

# ==========================
# INICIAR GRAVAÇÃO
# ==========================
def iniciar_gravacao():
    global gravando, listener_mouse, listener_keyboard

    passos.clear()
    atualizar_lista()

    janela.attributes("-topmost", True)

    contador = tk.Label(janela, text="", font=("Arial", 40), fg="red", bg="#F0F0F0")
    contador.place(relx=0.5, rely=0.4, anchor="center")

    def contagem():
        global gravando, listener_mouse, listener_keyboard

        for i in [3, 2, 1]:
            contador.config(text=str(i))
            time.sleep(1)

        contador.destroy()

        try: listener_mouse.stop()
        except: pass
        try: listener_keyboard.stop()
        except: pass

        gravando = True
        mostrar_indicador_gravando()

        listener_mouse = mouse.Listener(on_click=on_click, on_scroll=on_scroll)
        listener_keyboard = keyboard.Listener(on_press=on_key_press, on_release=on_key_release)

        listener_mouse.start()
        listener_keyboard.start()

    threading.Thread(target=contagem, daemon=True).start()

# ==========================
# PARAR GRAVAÇÃO
# ==========================
def parar_gravacao():
    global gravando, executando

    if executando:
        executando = False
        janela.attributes("-topmost", False)
        return

    if gravando:
        gravando = False
        esconder_indicador_gravando()

        janela.attributes("-topmost", False)

        if passos and passos[-1][0] in ("mouse_click", "double_click"):
            passos.pop()
            atualizar_lista()

        messagebox.showinfo("Fim", "Gravação finalizada!")

def esc_stop(key):
    global executando
    if key == keyboard.Key.esc and executando:
        executando = False

# ==========================
# EXECUÇÃO
# ==========================
def normalizar_caractere_ctrl(c):
    mapa = {
        "\x03": "c", "\x16": "v", "\x18": "x", "\x1a": "z",
        "\x01": "a", "\x02": "b", "\x04": "d", "\x05": "e",
        "\x06": "f", "\x07": "g", "\x08": "h", "\x09": "i",
        "\x0a": "j", "\x0b": "k", "\x0c": "l", "\x0d": "m",
        "\x0e": "n", "\x0f": "o", "\x10": "p", "\x11": "q",
        "\x12": "r", "\x13": "s", "\x14": "t", "\x15": "u",
        "\x17": "w", "\x19": "y",
    }
    return mapa.get(c, c)

def executar_automacao():
    global executando

    if not passos:
        messagebox.showerror("Erro", "Nenhum passo gravado!")
        return

    try:
        delay = float(entry_delay.get())
        repet = int(entry_repeticoes.get())
    except:
        messagebox.showerror("Erro", "Valores inválidos.")
        return

    messagebox.showinfo("Info", "Executando em 3 segundos...")
    time.sleep(3)

    executando = True

    for _ in range(repet):
        if not executando:
            break

        for p in passos:
            if not executando:
                break

            tipo = p[0]

            if tipo == "mouse_click":
                _, x, y, btn = p
                btn = "right" if "right" in btn else "middle" if "middle" in btn else "left"
                pyautogui.click(x, y, button=btn)

            elif tipo == "double_click":
                _, x, y, btn = p
                btn = "right" if "right" in btn else "middle" if "middle" in btn else "left"
                pyautogui.click(x, y, button=btn, clicks=2)

            elif tipo == "scroll":
                _, x, y, dx, dy = p
                pyautogui.moveTo(x, y)
                pyautogui.scroll(dy * 120)

            elif tipo == "key_press":
                _, k = p
                k = SPECIAL_KEYS.get(k, k)
                try: pyautogui.press(k)
                except: pass

            elif tipo == "hotkey":
                _, combo = p
                combo_exec = []
                for c in combo:
                    c = SPECIAL_KEYS.get(str(c), str(c))
                    c = normalizar_caractere_ctrl(c)
                    if c.startswith("'") and c.endswith("'"):
                        c = c[1:-1]
                    combo_exec.append(c)
                try:
                    pyautogui.hotkey(*combo_exec)
                except Exception as e:
                    print("Falha ao executar hotkey:", combo_exec, e)

            for _ in range(int(delay * 100)):
                if not executando:
                    break
                time.sleep(0.01)

    executando = False

# ==========================
# SALVAR / ABRIR
# ==========================
def salvar_automacao():
    if not passos:
        messagebox.showerror("Erro", "Nada para salvar!")
        return

    caminho = filedialog.asksaveasfilename(
        defaultextension=".json",
        filetypes=[("JSON", "*.json")],
        title="Salvar automação..."
    )
    if not caminho:
        return

    serializavel = []
    for p in passos:
        tipo = p[0]
        if tipo in ("mouse_click", "double_click"):
            _, x, y, btn = p
            serializavel.append({"tipo": tipo, "x": x, "y": y, "btn": btn})
        elif tipo == "scroll":
            _, x, y, dx, dy = p
            serializavel.append({"tipo": tipo, "x": x, "y": y, "dx": dx, "dy": dy})
        elif tipo == "key_press":
            _, k = p
            serializavel.append({"tipo": tipo, "key": k})
        elif tipo == "hotkey":
            _, combo = p
            serializavel.append({"tipo": tipo, "combo": combo})

    with open(caminho, "w", encoding="utf-8") as f:
        json.dump(serializavel, f, indent=2)

def abrir_automacao():
    global passos
    caminho = filedialog.askopenfilename(
        defaultextension=".json",
        filetypes=[("JSON", "*.json")],
        title="Abrir automação..."
    )
    if not caminho:
        return

    with open(caminho, "r", encoding="utf-8") as f:
        data = json.load(f)

    carregado = []
    for item in data:
        tipo = item["tipo"]
        if tipo in ("mouse_click", "double_click"):
            carregado.append((tipo, item["x"], item["y"], item["btn"]))
        elif tipo == "scroll":
            carregado.append(("scroll", item["x"], item["y"], item["dx"], item["dy"]))
        elif tipo == "key_press":
            carregado.append(("key_press", item["key"]))
        elif tipo == "hotkey":
            carregado.append(("hotkey", item["combo"]))

    passos = carregado
    atualizar_lista()

def atualizar_lista():
    lista.delete(0, tk.END)
    for p in passos:
        lista.insert(tk.END, str(p))

# ==========================
# UI — Windows 11 Modernizada
# ==========================

janela = tk.Tk()
janela.title("AutomaTudo")
janela.geometry("470x870")
janela.minsize(360, 460)
janela.configure(bg="#F5F7FA")   # fundo suave tipo Windows 11

# --------- Estilos Modernos ---------
COR_FUNDO = "#F5F7FA"
COR_CARD = "#FFFFFF"
COR_BOTAO = "#E1E8F5"
COR_BOTAO_ATIVO = "#CBD8ED"
COR_BOTAO_TEXTO = "#1A1A1A"
COR_TITULO = "#1A1A1A"
COR_LISTA = "#FAFAFA"
BORDA_CARD = "#D1D9E6"
AZUL = "#2563EB"

janela.option_add("*Font", ("Segoe UI", 10))

def criar_card(parent):
    frame = tk.Frame(
        parent,
        bg=COR_CARD,
        bd=1,
        relief="solid",
        highlightbackground=BORDA_CARD
    )
    return frame

def botao_moderno(parent, texto, comando):
    return tk.Button(
        parent,
        text=texto,
        command=comando,
        bg=COR_BOTAO,
        fg=COR_BOTAO_TEXTO,
        activebackground=COR_BOTAO_ATIVO,
        activeforeground=COR_BOTAO_TEXTO,
        font=("Segoe UI", 10, "bold"),
        relief="flat",
        height=1,
        bd=0,
        padx=20,
        pady=7,
        cursor="hand2"
    )

def mostrar_indicador_gravando():
    global gravando_indicator, animar_gravacao

    if gravando_indicator is not None:
        return  # já existe

    gravando_indicator = tk.Label(
        janela,
        text="● Gravando...",
        fg="red",
        bg="#F0F0F0",
        font=("Segoe UI", 12, "bold")
    )
    gravando_indicator.place(x=10, y=10)

    def blink():
        while animar_gravacao:
            current = gravando_indicator.cget("fg")
            novo = "#F0F0F0" if current == "red" else "red"
            gravando_indicator.config(fg=novo)
            time.sleep(0.5)

    threading.Thread(target=blink, daemon=True).start()

def esconder_indicador_gravando():
    global gravando_indicator, animar_gravacao
    animar_gravacao = False

    if gravando_indicator:
        gravando_indicator.destroy()
        gravando_indicator = None

    animar_gravacao = True

# --------- Título ---------
titulo = tk.Label(
    janela,
    text="Automação de Tarefas",
    font=("Segoe UI", 16, "bold"),
    bg=COR_FUNDO,
    fg=COR_TITULO
)
titulo.pack(pady=15)

# --------- CARD: Controles principais ---------
card_controles = criar_card(janela)
card_controles.pack(fill="x", padx=20, pady=10)

# tk.Label(card_controles, text="Controles de Gravação", bg=COR_CARD,
#          fg=AZUL, font=("Segoe UI", 11, "bold")).pack(pady=5)

frame_btns = tk.Frame(card_controles, bg=COR_CARD)
frame_btns.pack(pady=10)

botao_moderno(frame_btns, "🔴 Gravar", iniciar_gravacao).grid(row=0, column=0, padx=10)
botao_moderno(frame_btns, "⏹ Parar (Ctrl+Shift+S)", parar_gravacao).grid(row=0, column=1, padx=10)

# --------- CARD: Configurações ---------
card_cfg = criar_card(janela)
card_cfg.pack(fill="x", padx=20, pady=10)

# tk.Label(card_cfg, text="Configurações de Execução", bg=COR_CARD,
#          fg=AZUL, font=("Segoe UI", 11, "bold")).pack(pady=5)

frame = tk.Frame(card_cfg, bg=COR_CARD)
frame.pack(pady=10)

tk.Label(frame, text="Delay (s):", bg=COR_CARD, fg=COR_TITULO,
         font=("Segoe UI", 10, "bold")).grid(row=0, column=0, padx=5, sticky="e")

entry_delay = tk.Entry(frame, width=8)
entry_delay.grid(row=0, column=1, padx=5)
entry_delay.insert(0, "0.5")

tk.Label(frame, text="Repetições:", bg=COR_CARD, fg=COR_TITULO,
         font=("Segoe UI", 10, "bold")).grid(row=1, column=0, padx=5, sticky="e")

entry_repeticoes = tk.Entry(frame, width=8)
entry_repeticoes.grid(row=1, column=1, padx=5)
entry_repeticoes.insert(0, "1")

botao_moderno(card_cfg, "▶ Executar Automação", executar_automacao).pack(pady=12)

# --------- CARD: Salvar/Abrir ---------
card_arquivos = criar_card(janela)
card_arquivos.pack(fill="x", padx=20, pady=10)

# tk.Label(card_arquivos, text="Salvar / Abrir Automação", bg=COR_CARD,
#          fg=AZUL, font=("Segoe UI", 11, "bold")).pack(pady=5)

frame_sav = tk.Frame(card_arquivos, bg=COR_CARD)
frame_sav.pack(pady=10)

botao_moderno(frame_sav, "💾 Salvar", salvar_automacao).grid(row=0, column=0, padx=10)
botao_moderno(frame_sav, "📂 Abrir", abrir_automacao).grid(row=0, column=1, padx=10)

# --------- CARD: Lista de passos ---------
card_lista = criar_card(janela)
card_lista.pack(expand=True, fill="both", padx=20, pady=10)

tk.Label(card_lista, text="Passos Gravados", bg=COR_CARD,
         fg=AZUL, font=("Segoe UI", 11, "bold")).pack(pady=5)

frame_lista = tk.Frame(card_lista, bg=COR_CARD)
frame_lista.pack(expand=True, fill="both", padx=10, pady=10)

scroll = tk.Scrollbar(frame_lista)
scroll.pack(side="right", fill="y")

lista = tk.Listbox(
    frame_lista,
    font=("Segoe UI", 10),
    bg=COR_LISTA,
    bd=0,
    relief="flat",
    highlightthickness=1,
    highlightbackground=BORDA_CARD,
    yscrollcommand=scroll.set
)
lista.pack(side="left", expand=True, fill="both")
scroll.config(command=lista.yview)

botao_moderno(card_lista, "🗑 Remover passo selecionado",
              lambda: (passos.pop(lista.curselection()[0]), atualizar_lista())
              if lista.curselection() else None).pack(pady=10)

# Listener ESC
listener_stop = keyboard.Listener(on_press=esc_stop)
listener_stop.start()

marca = tk.Label(
    janela,
    text="by Jeferson Santos",
    font=("Segoe UI", 8),
    fg="#999999"
)
marca.pack(side="bottom", pady=5)

janela.mainloop()
