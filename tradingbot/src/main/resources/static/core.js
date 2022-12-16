const activity = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-activity" viewBox="0 0 16 16">' +
    '<path fill-rule="evenodd" d="M6 2a.5.5 0 0 1 .47.33L10 12.036l1.53-4.208A.5.5 0 0 1 12 7.5h3.5a.5.5 0 0 1 0 1h-3.15l-1.88 5.17a.5.5 0 0 1-.94 0L6 3.964 4.47 8.171A.5.5 0 0 1 4 8.5H.5a.5.5 0 0 1 0-1h3.15l1.88-5.17A.5.5 0 0 1 6 2Z"/>' +
    '</svg>'
const ttl = '<div class="row" style="width: 100%">' +
    '<div class="col-1"><span class="badge rounded-pill text-bg-{{badge}}">{{side}}</span></div>' +
    '<div class="col-4">{{start}} ~ {{end}}</div>' +
    '<div class="col-1">{{ttlSz}}</div>' +
    '<div class="col-2 text-truncate">{{startPx}} ' + activity + ' {{endPx}}</div>' +
    '<div class="col-2 text-truncate">{{pnl}}</div></div>'
const detail = '<a href="#" class="list-group-item list-group-item-action">{{detail}}</a>'
const accordion = '<div class="accordion mt-3">{{body}}</div>'
const card = '<div class="accordion-item">' +
    '<h2 class="accordion-header" id="{{headingId}}">' +
    '<button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#{{collapseId}}" aria-expanded="false" aria-controls="{{collapseId}}">{{title}}</button>' +
    '</h2>' +
    '<div id="{{collapseId}}" class="accordion-collapse collapse" aria-labelledby="{{headingId}}" data-bs-parent="#list">' +
    '<div class="accordion-body"><div class="list-group">{{content}}</div></div></div></div>'

let collapseIdx = 0
let headingIdx = 0

function createDetail(o) {
    const d = o.time + ' ' + o.side + ' ' + o.sz + ' ' + o.px + ' tp=' + o.tp + ' sl=' + o.sl
    return detail.replace('{{detail}}', d)
}

function createCard(mo) {
    collapseIdx++
    headingIdx++
    const end = mo.end === 'now' ? mo.end : mo.end.substring(11, 19)
    const title = ttl.replace('{{badge}}', mo.side === 'long' ? 'success' : 'danger')
        .replace('{{side}}', mo.side.toUpperCase())
        .replace('{{start}}', mo.start.substring(0, 19))
        .replace('{{end}}', end)
        .replace('{{ttlSz}}', mo.ttlSz)
        .replace('{{startPx}}', mo.startPx)
        .replace('{{endPx}}', mo.endPx)
        .replace('{{pnl}}', mo.pnl)
    const details = []
    for (let o of mo.orders)
        details.push(createDetail(o))
    return card.replace('{{title}}', title)
        .replace('{{content}}', details.join(''))
        .replaceAll('{{collapseId}}', 'collapse' + collapseIdx.toString())
        .replaceAll('{{headingId}}', 'heading' + headingIdx.toString())
}

function displayAccordion(mos) {
    const accordions = []
    let cards = []
    let date = ''
    for (let mo of mos) {
        const d = mo.start.substring(0, 10)
        if (date === '')
            date = d
        if (date !== d) {
            accordions.push(accordion.replace('{{body}}', cards.join('')))
            cards = []
            date = d
        }
        cards.push(createCard(mo))
    }
    if (accordions.length === 0)
        accordions.push(accordion.replace('{{body}}', cards.join('')))

    const $board = $('#board')
    $board.empty()
    $board.append(accordions.join(''))
}

const Order = function() {
    this.px = ''
    this.sz = 0
    this.side = ''
    this.time = ''
    this.algoSz = 0
    this.tp = ''
    this.sl = ''
}

const MartinOrder = function() {
    this.start = ''
    this.end = ''
    this.side = ''
    this.startPx = ''
    this.endPx = ''
    this.ttlSz = 0
    this.cost = ''
    this.pnl = ''
    this.orders = []
}

const sumSz = function(martinOrder) {
    let ttlSz = 0
    for (let o of martinOrder.orders)
        ttlSz += o.sz
    return ttlSz
}

const doAnalysis = function (event) {
    const lines = event.target.result.split('\n')
    const costRegex = /.*Initiator: \d+ cost (\w+(.\w+)?) at.*/
    const fillRegex = /.*sz=(\w+), avgPx=(\w+(.\w+)?), posSide=(\w+), accFillSz=(\w+), state=(\w+), side=(\w+)}/
    const algoRegex = /.*tp=(\w+(.\w+)?) sl=(\w+(.\w+)?).*/
    const martinOrders = []
    let martinOrder = new MartinOrder()
    let order = new Order()
    for (let line of lines) {
        if (line.indexOf('o.i.m') < 0)
            continue

        if (line.indexOf('close by take profit at ') > -1) {
            martinOrder.end = line.substring(0, 23)
            martinOrder.endPx = line.substring(line.indexOf('at ')+3)
            martinOrder.ttlSz = sumSz(martinOrder)
            martinOrders.push(martinOrder)
            martinOrder = new MartinOrder()
            martinOrder.end = 'now'
            martinOrder.endPx = '?'
        }
        if (line.indexOf('cost') > -1) {
            const matcher = costRegex.exec(line);
            if (matcher === null)
                throw new Error('match cost line error ' + line)
            martinOrder.cost = matcher[1]
        }
        if (line.indexOf('Tracker: filled data') > -1) {    // shadow
            const matcher = fillRegex.exec(line);
            if (matcher === null)
                throw new Error('match filled line error ' + line)
            order.px = matcher[2]
            order.sz = parseInt(matcher[1])
            order.side = matcher[4]
            order.time = line.substring(0, 23)
            martinOrder.orders.push(order)
            if (order.sz === 10) {
                martinOrder.start = order.time
                martinOrder.side = order.side
                martinOrder.startPx = order.px
            }
            order = new Order()
        }
        if (line.indexOf('Recorder: filled order') > -1) {  // test
            const arr = line.substring(line.indexOf('filled order ') + 13).split(' ');
            if (martinOrder.start !== '' && sumSz(martinOrder) === parseInt(arr[2])) {
                martinOrder.end = line.substring(0, 23)
                martinOrder.endPx = arr[3]
                martinOrder.ttlSz = sumSz(martinOrder)
                continue
            }
            order.px = arr[3]
            order.sz = parseInt(arr[2])
            order.side = arr[0]
            order.time = line.substring(0, 23)
            martinOrder.orders.push(order)
            if (order.sz === 10) {
                martinOrder.start = order.time
                martinOrder.side = order.side
                martinOrder.startPx = order.px
            }
            order = new Order()
        }
        if (line.indexOf('o.i.m.t.o.c.Initiator') > -1 && line.indexOf('batch order closed') > -1) {  // test
            martinOrder.pnl = line.substring(line.indexOf('balance'), line.lastIndexOf(',')).split(' ')[2]
            martinOrders.push(martinOrder)
            martinOrder = new MartinOrder()
            martinOrder.end = 'now'
            martinOrder.endPx = '?'
        }
        if (line.indexOf('placed algo') > -1) {
            const matcher = algoRegex.exec(line);
            if (matcher === null)
                throw new Error('match algo line error ' + line)
            order.tp = matcher[1]
            order.sl = matcher[3]
        }
    }
    if (martinOrder.start !== '') {
        martinOrder.ttlSz = sumSz(martinOrder)
        martinOrders.push(martinOrder)
    }

    displayAccordion(martinOrders)
}

const analysis = function () {
    const file = $('#file')[0].files[0]
    const fileReader = new FileReader()
    fileReader.onload = doAnalysis
    fileReader.readAsText(file, 'utf-8')
};


