/* Escapes question text and renders the light $...$ notation used in the bank. */
Aden.mathText = (() => {
  const MAP = [
    [/\\tan/g, "tan"], [/\\sin/g, "sin"], [/\\cos/g, "cos"], [/\\cot/g, "cot"], [/\\sec/g, "sec"], [/\\csc/g, "csc"],
    [/\\theta/g, "θ"], [/\\pi/g, "π"], [/\\infty/g, "∞"], [/\\sqrt\{([^}]+)\}/g, "√($1)"], [/\\frac\{([^}]+)\}\{([^}]+)\}/g, "($1/$2)"],
    [/\\times/g, "×"], [/\\div/g, "÷"], [/\\pm/g, "±"], [/\\le/g, "≤"], [/\\ge/g, "≥"], [/\\neq/g, "≠"], [/\\rightarrow/g, "→"],
    [/\^2/g, "²"], [/\^3/g, "³"], [/\\/g, ""],
  ];
  return (str) => {
    const safe = Aden.util.esc(str);
    return safe.replace(/\$([^$]+)\$/g, (_, m) => `<span class="math">${MAP.reduce((s, [re, to]) => s.replace(re, to), m)}</span>`);
  };
})();
