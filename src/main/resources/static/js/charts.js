/* ============================================================
   轻量 Canvas 图表：折线 / 柱状 / 环形
   不引入任何第三方图表库，离线可用
   ============================================================ */
(function () {
    'use strict';

    var PALETTE = ['#0d9488', '#f59e0b', '#2563eb', '#8b5cf6', '#ec4899', '#14b8a6', '#ef4444', '#64748b'];

    function setup(canvas, height) {
        var dpr = window.devicePixelRatio || 1;
        var width = canvas.parentElement.clientWidth || 480;
        canvas.width = width * dpr;
        canvas.height = height * dpr;
        canvas.style.width = width + 'px';
        canvas.style.height = height + 'px';
        var ctx = canvas.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, width, height);
        ctx.font = '12px -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif';
        return { ctx: ctx, width: width, height: height };
    }

    function tooltip(container) {
        var tip = container.querySelector('.chart-tip');
        if (!tip) {
            tip = document.createElement('div');
            tip.className = 'chart-tip';
            container.appendChild(tip);
        }
        return tip;
    }

    function niceMax(value) {
        if (value <= 5) return 5;
        var exp = Math.pow(10, Math.floor(Math.log10(value)));
        var base = Math.ceil(value / exp);
        return base * exp;
    }

    /* ---------------- 折线图 ---------------- */
    function line(canvas, labels, series, options) {
        options = options || {};
        var h = options.height || 240;
        var box = setup(canvas, h);
        var ctx = box.ctx, w = box.width, hh = box.height;
        var padL = 34, padR = 12, padT = 16, padB = 28;
        var maxValue = 0;
        series.forEach(function (s) { s.data.forEach(function (v) { maxValue = Math.max(maxValue, v); }); });
        var top = niceMax(maxValue);
        var plotW = w - padL - padR, plotH = hh - padT - padB;

        ctx.strokeStyle = '#eef3f6';
        ctx.fillStyle = '#8b9caa';
        ctx.lineWidth = 1;
        for (var i = 0; i <= 4; i++) {
            var y = padT + plotH * i / 4;
            ctx.beginPath();
            ctx.moveTo(padL, y);
            ctx.lineTo(w - padR, y);
            ctx.stroke();
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            ctx.fillText(String(Math.round(top - top * i / 4)), padL - 7, y);
        }

        var stepX = labels.length > 1 ? plotW / (labels.length - 1) : 0;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        labels.forEach(function (label, i) {
            if (labels.length > 10 && i % 2 !== 0) return;
            ctx.fillStyle = '#8b9caa';
            ctx.fillText(label, padL + stepX * i, hh - padB + 8);
        });

        series.forEach(function (s, si) {
            var color = s.color || PALETTE[si % PALETTE.length];
            ctx.strokeStyle = color;
            ctx.lineWidth = 2.4;
            ctx.lineJoin = 'round';
            ctx.beginPath();
            s.data.forEach(function (v, i) {
                var x = padL + stepX * i;
                var y = padT + plotH - (v / top) * plotH;
                if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
            });
            ctx.stroke();

            var grad = ctx.createLinearGradient(0, padT, 0, padT + plotH);
            grad.addColorStop(0, hexA(color, .22));
            grad.addColorStop(1, hexA(color, 0));
            ctx.lineTo(padL + stepX * (s.data.length - 1), padT + plotH);
            ctx.lineTo(padL, padT + plotH);
            ctx.closePath();
            ctx.fillStyle = grad;
            ctx.fill();

            s.data.forEach(function (v, i) {
                var x = padL + stepX * i;
                var y = padT + plotH - (v / top) * plotH;
                ctx.beginPath();
                ctx.arc(x, y, 3.2, 0, Math.PI * 2);
                ctx.fillStyle = '#fff';
                ctx.fill();
                ctx.strokeStyle = color;
                ctx.lineWidth = 2;
                ctx.stroke();
            });
        });

        bindHover(canvas, labels, series, padL, stepX, plotH, padT);
    }

    /* ---------------- 柱状图 ---------------- */
    function bar(canvas, labels, values, options) {
        options = options || {};
        var h = options.height || 240;
        var box = setup(canvas, h);
        var ctx = box.ctx, w = box.width, hh = box.height;
        var padL = 34, padR = 12, padT = 16, padB = 34;
        var top = niceMax(Math.max.apply(null, values.concat([1])));
        var plotW = w - padL - padR, plotH = hh - padT - padB;
        var slot = plotW / labels.length;
        var barW = Math.min(38, slot * 0.52);

        ctx.strokeStyle = '#eef3f6';
        for (var i = 0; i <= 4; i++) {
            var y = padT + plotH * i / 4;
            ctx.beginPath();
            ctx.moveTo(padL, y);
            ctx.lineTo(w - padR, y);
            ctx.stroke();
            ctx.fillStyle = '#8b9caa';
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            ctx.fillText(String(Math.round(top - top * i / 4)), padL - 7, y);
        }

        values.forEach(function (v, i) {
            var x = padL + slot * i + (slot - barW) / 2;
            var barH = (v / top) * plotH;
            var y = padT + plotH - barH;
            var grad = ctx.createLinearGradient(0, y, 0, padT + plotH);
            grad.addColorStop(0, options.color || '#0d9488');
            grad.addColorStop(1, hexA(options.color || '#0d9488', .55));
            roundRect(ctx, x, y, barW, Math.max(barH, 2), 6);
            ctx.fillStyle = grad;
            ctx.fill();
            ctx.fillStyle = '#5b7383';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'top';
            ctx.fillText(labels[i], padL + slot * i + slot / 2, hh - padB + 9);
            if (v > 0) {
                ctx.fillStyle = '#334c5b';
                ctx.textBaseline = 'bottom';
                ctx.fillText(String(v), padL + slot * i + slot / 2, y - 4);
            }
        });
    }

    /* ---------------- 环形图 ---------------- */
    function donut(canvas, data, options) {
        options = options || {};
        var h = options.height || 220;
        var box = setup(canvas, h);
        var ctx = box.ctx, w = box.width, hh = box.height;
        var cx = w / 2, cy = hh / 2, radius = Math.min(w, hh) / 2 - 12, inner = radius * .62;
        var total = data.reduce(function (sum, d) { return sum + d.value; }, 0) || 1;
        var start = -Math.PI / 2;

        data.forEach(function (d, i) {
            var angle = d.value / total * Math.PI * 2;
            ctx.beginPath();
            ctx.arc(cx, cy, radius, start, start + angle);
            ctx.arc(cx, cy, inner, start + angle, start, true);
            ctx.closePath();
            ctx.fillStyle = d.color || PALETTE[i % PALETTE.length];
            ctx.fill();
            start += angle;
        });

        ctx.fillStyle = '#16232b';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.font = '600 20px -apple-system, "PingFang SC", sans-serif';
        ctx.fillText(String(total), cx, cy - 6);
        ctx.font = '12px -apple-system, "PingFang SC", sans-serif';
        ctx.fillStyle = '#8b9caa';
        ctx.fillText(options.unit || '总订单', cx, cy + 14);
    }

    /* ---------------- 辅助 ---------------- */
    function roundRect(ctx, x, y, w, h, r) {
        var radius = Math.min(r, w / 2, h / 2);
        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.lineTo(x + w - radius, y);
        ctx.quadraticCurveTo(x + w, y, x + w, y + radius);
        ctx.lineTo(x + w, y + h - radius);
        ctx.quadraticCurveTo(x + w, y + h, x + w - radius, y + h);
        ctx.lineTo(x + radius, y + h);
        ctx.quadraticCurveTo(x, y + h, x, y + h - radius);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
        ctx.closePath();
    }

    function hexA(hex, alpha) {
        var c = hex.replace('#', '');
        if (c.length === 3) c = c.split('').map(function (x) { return x + x; }).join('');
        var num = parseInt(c, 16);
        return 'rgba(' + [(num >> 16) & 255, (num >> 8) & 255, num & 255].join(',') + ',' + alpha + ')';
    }

    function bindHover(canvas, labels, series, padL, stepX, plotH, padT) {
        var container = canvas.parentElement;
        var tip = tooltip(container);
        var points = [];
        series.forEach(function (s, si) {
            s.data.forEach(function (v, i) {
                points.push({ x: padL + stepX * i, y: padT + plotH, label: labels[i], value: v, name: s.name, color: s.color || PALETTE[si] });
            });
        });
        canvas.addEventListener('mousemove', function (e) {
            var rect = canvas.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var best = null, bestDist = 26;
            points.forEach(function (p) {
                var d = Math.abs(p.x - mx);
                if (d < bestDist) { best = p; bestDist = d; }
            });
            if (best) {
                tip.innerHTML = best.label + ' · ' + best.name + ' <b>' + best.value + '</b>';
                tip.style.left = best.x + 'px';
                tip.style.top = best.y + 'px';
                tip.style.opacity = '1';
            } else {
                tip.style.opacity = '0';
            }
        });
        canvas.addEventListener('mouseleave', function () { tip.style.opacity = '0'; });
    }

    window.SimpleChart = { line: line, bar: bar, donut: donut, palette: PALETTE };
})();
