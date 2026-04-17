#pragma once

#include <gc/gc.h>

#include <jtl/immutable_string.hpp>
#include <jtl/primitive.hpp>

#include <jank/runtime/core/make_box.hpp>
#include <jank/runtime/object.hpp>
#include <jank/runtime/obj/opaque_box.hpp>
#include <jank/runtime/obj/persistent_hash_map.hpp>
#include <jank/runtime/obj/persistent_string.hpp>
#include <jank/runtime/obj/transient_hash_map.hpp>

#include "httplib.h"

namespace portal::runtime::jank::server
{

  using namespace ::jank::runtime;

  httplib::Server *create_server(object_ref const handler)
  {
    auto const server = new (UseGC) httplib::Server();

    auto http_handler(
        [=](const httplib::Request &req, httplib::Response &res)
        {
          if (req.has_header("Upgrade") && req.get_header_value("Upgrade") == "websocket")
          {
            return httplib::Server::HandlerResponse::Unhandled;
          }

          struct GC_stack_base sb;
          GC_get_stack_base(&sb);
          GC_register_my_thread(&sb);

          auto const req_type(jtl::immutable_string("httplib::Request*"));
          auto const res_type(jtl::immutable_string("httplib::Response*"));

          jtl::ptr<void> req_val{const_cast<httplib::Request *>(&req)};
          jtl::ptr<void> res_val{const_cast<httplib::Response *>(&res)};

          auto const boxed_req(make_box<obj::opaque_box>(req_val, req_type));
          auto const boxed_res(make_box<obj::opaque_box>(res_val, res_type));

          handler.call(boxed_req, boxed_res, jank_nil);

          GC_unregister_my_thread();
          return httplib::Server::HandlerResponse::Handled;
        });

    server->Get(R"(.*)", http_handler);
    server->Post(R"(.*)", http_handler);
    server->Put(R"(.*)", http_handler);
    server->Delete(R"(.*)", http_handler);
    server->Patch(R"(.*)", http_handler);

    server->WebSocket(
        R"(.*)",
        [=](const httplib::Request &req, httplib::ws::WebSocket &ws)
        {
          struct GC_stack_base sb;
          GC_get_stack_base(&sb);
          GC_register_my_thread(&sb);

          auto const req_type(jtl::immutable_string("httplib::Request*"));
          auto const ws_type(jtl::immutable_string("httplib::ws::WebSocket*"));

          jtl::ptr<void> req_val{const_cast<httplib::Request *>(&req)};
          jtl::ptr<void> ws_val{const_cast<httplib::ws::WebSocket *>(&ws)};

          auto const boxed_req(make_box<obj::opaque_box>(req_val, req_type));
          auto const boxed_ws(make_box<obj::opaque_box>(ws_val, ws_type));

          handler.call(boxed_req, jank_nil, boxed_ws);

          GC_unregister_my_thread();
        });

    return server;
  }

  object_ref request_headers(httplib::Request const *req)
  {
    obj::transient_hash_map m{};
    for (const auto &[k, v] : req->headers)
    {
      m.assoc_in_place(make_box<obj::persistent_string>(k),
                       make_box<obj::persistent_string>(v));
    }
    return m.to_persistent();
  }

  void destroy_server(httplib::Server *server)
  {
    server->stop();
  }

}